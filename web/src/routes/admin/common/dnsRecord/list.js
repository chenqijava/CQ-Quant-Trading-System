import React, { Component } from 'react'

import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Row,
  Col,
  Button,
  Modal,
  Select,
  Radio
} from 'antd'

import {connect} from 'dva'
import axios from 'axios'
import { render } from 'nprogress';
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip,} from 'tdesign-react';

class DnsRecord extends Component {
    // 当前页参数
    constructor(props) {
        super(props);
        this.state = {
            // 数据存储
            data: [],
            // 状态记录
            loading: false,
            addVisible:false,
            // 页面参数
            pagination: {
              pageSize: 10,
              current: 1,
              showTotal: (total, range) => `共 ${total} 条`,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500'],
            },
            // 筛选参数
            selectName: '',
            selectdomain: '',
            // 选中的数据
            selectedRowKeys: [],
            // 设置表格参数，默认为0
            tableContentHeight: 0,
            scrollY: 0,
            selectedRows: null,
            // 新增所需的参数
            paths:'',
            workName: '',
            num: '',
            target: '',
            type: "0",
            // 查看当前行数据所需参数
            checkVisible: false,
            currentRecord: null,
        }
        this.selectedRows = []
        this.filters = {}
        this.sorter = {}
    }
    // 格式化时间
    formatBeijingTime = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        // 保持原有 UTC 时间显示
        return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}-${String(date.getUTCDate()).padStart(2, '0')} ${String(date.getUTCHours()).padStart(2, '0')}:${String(date.getUTCMinutes()).padStart(2, '0')}:${String(date.getUTCSeconds()).padStart(2, '0')}`;
    }

    // 选择数据行的状态变化
    async onRowSelectionChange(selectedRowKeys, selectedRows) {
        // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
        this.setState({selectedRowKeys})
        this.selectedRows = selectedRows
    }
    // 对表格分页、排序、筛选变化时的统一回调处理
    async handleTableChange(pagination, filters, sorter) {
        let sort = this.sorter
        if (sorter && sorter.field) {
            sort = {}
            sort[sorter.field] = sorter.order == 'descend' ? -1 : 1
        }

        let oldFilters = this.filters
        for (let key in filters) {
            if (filters[key].length > 0) {
                let filter = filters[key]
                oldFilters[key] = {$in: filter}
            } else {
                delete oldFilters[key]
            }
        }

        // 直接从所有数据中计算当前页数据，而不是重新请求
        this.load(pagination)
        this.filters = oldFilters
        this.sorter = sort
    }

    // 更新表格的高
    handleResize = () => {
        let height = document.body.getBoundingClientRect().height;
        if (this.state.selectedCountRef) {
          setTimeout(() => {
            if (this.state.selectedCountRef) {
              this.setState({
                tableContentHeight: height - this.state.selectedCountRef.getBoundingClientRect().top - 84,
                scrollY: height - this.state.selectedCountRef.getBoundingClientRect().top - 84 - 80
              })
            }
          }, 100)
        }
    }

    refSelectedCount = (ref) => {
        this.state.selectedCountRef = ref
        this.setState({ selectedCountRef: ref })
        this.handleResize()
    }

    // 首次加载数据
    async componentWillMount() {
        this.reload()
    }
    async componentDidMount() {
        window.addEventListener("resize", this.handleResize);
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.handleResize);
    }

    async reload() {
        // 重新加载，一般是页面第一次加载的时候来一下
        this.state.pagination.current = 1
        this.setState({pagination: this.state.pagination})
        this.load(this.state.pagination)
    }
    // 获取数据
    async load(pagination) {
        this.setState({loading: true})
            // 构建查询参数
        const params = {
        };

        // 添加过滤条件
        if (this.filters.selectdomain) {
            params.domain = this.filters.selectdomain;
        }
        if (this.filters.selectName) {
            params.workName = this.filters.selectName;
        }
        let res = await axios.get(`/api/dns/getURLtoDomain/${pagination.current}/${pagination.pageSize}`, { params: params })

        // 根据后端返回结构调整数据处理
        const records = res.data.data.records || []
        const totalCount = res.data.data.totalCount || 0

        this.setState({
            loading: false,
            data: records,
            pagination: { ...pagination, total: totalCount },
            selectedRowKeys: []
        })

        // 处理当前页数据
        this.selectedRows = []
    }

    async reset() {
        this.filters = {}
        this.state.pagination.current = 1
        this.setState({ filters: {}, pagination: { ...this.state.pagination }, createTimeRange: ['', ''], selectName: '', selectdomain: '' })

        this.load(this.state.pagination, this.filters, this.sorter)
    }

    // 新增方法
    async openAdd() {
        this.setState({addVisible: true})
    }

    // 删除方法
    async delete() {
        // 检查是否有选中的行
        if (this.state.selectedRowKeys.length === 0) {
            message.error('请先选择要删除的数据');
            return;
        }

        // 添加删除确认提示框
        const confirmResult = await new Promise((resolve) => {
            Modal.confirm({
                title: '确认删除',
                content: `确定要删除选中的 ${this.selectedRows.length} 条记录吗？`,
                okText: '确认',
                cancelText: '取消',
                onOk: () => resolve(true),
                onCancel: () => resolve(false)
            });
        });

        if (!confirmResult) {
            return; // 用户取消删除
        }

        // 遍历选中的selectedRows获取所有id
        const workNames = this.selectedRows.map(row => row.workName);
        const domains = this.selectedRows.map(row => row.domain);
        // 调用后端删除接口
        try {
            this.setState({ loading: true });
            let res = await axios.delete(`/api/dns/deleteUrlToDomain`,{
                params: {
                    workNames: workNames,
                    domains: domains
                },
                paramsSerializer: params => {
                    // 手动构建重复参数格式
                    return Object.keys(params)
                    .map(key => {
                        if (Array.isArray(params[key])) {
                            // 修改这里：将内层变量名从 key 改为 value
                            return params[key].map(value => `${key}=${encodeURIComponent(value)}`).join('&');
                        }
                        return `${key}=${encodeURIComponent(params[key])}`;
                    })
                    .join('&');
                }
            });
            if (res.data.code === 1) {
                message.success('删除成功');
                // 重新加载数据
                this.reload();
            } else {
                message.error(res.data.message || '删除失败');
            }
        } catch (error) {
            message.error('删除过程中发生错误');
            console.error(error);
        } finally {
            this.setState({ loading: false });
        }
    }

    render() {

        const columns = [
            { title: '任务名', dataIndex: 'workName', key: 'workName', width: '20%'  },
            { title: '目标URL', dataIndex: 'domain', key: 'domain', width: '20%'  },
            { title: '链接数', dataIndex: 'count', key: 'linkNum', width: '10%' },
            { title: '垃圾率', dataIndex: 'rate', key: 'rate', width: '10%', render: (v) => {return (v * 100) + '%'}  },
            {
                title: '创建时间',
                dataIndex: 'latestCreateTime',
                key: 'createTime',
                width: '20%',
                render: (text) => {
                    return this.formatBeijingTime(text);
                }
            },
            {
                title: '操作',
                dataIndex: 'oper',
                key: 'oper',
                render: (v, r) => {
                  return <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px', // 按钮间距
                      padding: '8px 0 8px 8px', // 上下内边距8px，左侧内边距16px
                      marginLeft: '8px' // 增加左侧外边距
                    }}>
                      <Button type="link"
                        style={{
                          padding: 0,
                          minWidth: 'auto',
                        }} onClick={() => {
                            this.setState({
                                currentRecord: r,
                                checkVisible: true
                            });
                        }}>查看</Button>
                    <Button type="link"
                        style={{
                          padding: 0,
                          minWidth: 'auto',
                        }} onClick={() => {
                            try{
                                let txtContent = `${r.dnsList.join('\n')}`
                                const blob = new Blob([txtContent], { type: 'text/plain;charset=utf-8' });
                                const url = URL.createObjectURL(blob);
                                const link = document.createElement('a');
                                link.href = url;
                                link.setAttribute('download', `${r.workName}+${r.domain}.txt`);
                                document.body.appendChild(link);
                                link.click();
                                document.body.removeChild(link);
                                URL.revokeObjectURL(url);
                                message.success('导出成功');
                            }catch(e){
                                message.error('导出失败');
                                console.error(e);
                            }

                        }}>导出</Button>
                    </div>
                }
            },
        ]

        return (
            <MyTranslate><ExternalAccountFilter>
                <Breadcrumb>
                    <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
                    <Breadcrumb.BreadcrumbItem>DNS域名映射</Breadcrumb.BreadcrumbItem>
                </Breadcrumb>

                <div className="account-search-box">

                    <div className='account-search-item'>
                        <div className="account-search-item-label">任务名</div>
                        <div className="account-search-item-right">
                            <Input
                                allowClear
                                style={{ width: 200 }}
                                placeholder="请输入"
                                value={this.state.selectName}
                                onChange={e => {
                                    this.setState({ selectName: e.target.value })
                                    this.filters['selectName'] = e.target.value.trim()
                                    }
                                }
                                onPressEnter={e => {
                                    this.setState({ selectName: e.target.value })
                                    this.filters['selectName'] = e.target.value.trim()
                                    this.reload()
                                    }
                                }
                            />
                        </div>
                    </div>
                    <div className='account-search-item'>
                        <div className="account-search-item-label">目标URL</div>
                        <div className="account-search-item-right">
                            <Input
                                allowClear
                                style={{ width: 200 }}
                                placeholder="请输入"
                                value={this.state.selectdomain}
                                onChange={e => {
                                    this.setState({ selectdomain: e.target.value })
                                    this.filters['selectdomain'] = e.target.value.trim()
                                    }
                                }
                                onPressEnter={e => {
                                    this.setState({ selectdomain: e.target.value })
                                    this.filters['selectdomain'] = e.target.value.trim()
                                    this.reload()
                                    }
                                }
                            />
                        </div>
                    </div>

                    <div className='account-btn-no-expand' style={{ width: '136px' }}>
                        <div style={{ display: 'flex', justifyContent: 'right', alignItems: 'center' }}>
                            <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
                            <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
                        </div>
                    </div>
                    <div style={{ clear: 'both' }}></div>
                </div>

                <div className="account-main-content">
                    <div style={{overflow: 'hidden'}}>

                        <div className="table-operations">
                            <div className={"search-query-btn"} onClick={() => {this.openAdd()}}>新增</div>
                            <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                                onClick={() => { this.delete() }}>批量删除
                            </div>
                        </div>
                        <div className="tableSelectedCount" ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
                        <div className="tableContent" style={{height: this.state.tableContentHeight}}>
                            <div>
                                <Table
                                    tableLayout="fixed"
                                    scroll={{
                                        y: this.state.scrollY,
                                        x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b
                                    )}}
                                    pagination={this.state.pagination}
                                    rowSelection={{
                                        selectedRowKeys: this.state.selectedRowKeys,
                                        onChange: this.onRowSelectionChange.bind(this)
                                    }}
                                    columns={columns}
                                    rowKey='_id'
                                    dataSource={this.state.data}
                                    loading={this.state.loading}
                                    onChange={this.handleTableChange.bind(this)}
                                />
                            </div>
                        </div>
                        <Pagination
                            showJumper
                            total={this.state.pagination.total}
                            current={this.state.pagination.current}
                            pageSize={this.state.pagination.pageSize}
                            onChange={this.handleTableChange.bind(this)}
                        />
                    </div>
                </div>

                <Dialog
                    header="新增"
                    width={854}
                    height={566}
                    placement='center'
                    visible={this.state.addVisible}
                    onConfirm={async () => {
                        console.log(`${this.state.paths} * ${this.state.num}`)
                        if (this.state.type === "1") {
                          this.setState({addVisible:false})
                          return
                        }
                        let pathsArray = this.state.paths.split('\n').filter(path => path.trim() !== '');
                        let res = await axios.post(`api/dns/addUrlToDomain/${this.state.num}`, {
                            workName: this.state.workName,
                            paths: pathsArray,
                            target: this.state.target
                        })
                        if (res.data.code === 1) {
                            message.success('新增成功');
                            this.reload();
                        } else {
                            message.error(res.data.message || '新增失败');
                        }
                        this.setState({addVisible:false})
                    }}
                    confirmLoading={this.state.loading}
                    onCancel={()=>{this.setState({addVisible:false, workName:'', paths: '', num: '', target: '', type: '0'})}}
                    onClose={()=>{this.setState({addVisible:false, workName:'', paths: '', num: '', target: '', type: '0'})}}
                >
                    <div style={{marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
                        <div style={{display: 'flex', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>任务名称
                            </div>
                            <div>
                                <Input value={this.state.workName}
                                    onChange={(e) => this.setState({workName: e.target.value})} style={{width: 400}}
                                    placeholder='请输入'
                                />
                            </div>
                        </div>
                        <div style={{display: 'flex', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>链接生成方式
                            </div>
                            <div>
                                <Radio.Group value={this.state.type} disabled={!!this.state.editUser} onChange={(e) => {
                                this.setState({ type: e.target.value})
                                }}>
                                                <Radio value={"0"}>自选生成方式</Radio>
                                                <Radio value={"1"}>智能风控推荐</Radio>
                                              </Radio.Group>
                            </div>
                        </div>
                        <div style={{display: this.state.type === "0" ? 'flex': 'none', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>域名选择
                            </div>
                            <div>

                                 <Select style={{width: 160}} value={this.state.target} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ target: v })
                                  }}>
                                     <Option value="inboxtab.com">inboxtab.com</Option>
                                     <Option value="mailnesta.com">mailnesta.com</Option>
                                     <Option value="postybox.com">postybox.com</Option>
                                     <Option value="zapcourierr.com">zapcourierr.com</Option>
                                     <Option value="letterloopf.com">letterloopf.com</Option>
                                     <Option value="sendorae.com">sendorae.com</Option>
                                     <Option value="mailyardb.com">mailyardb.com</Option>
                                     <Option value="inboxerly.com">inboxerly.com</Option>
                                     <Option value="cloudstampp.com">cloudstampp.com</Option>
                                     <Option value="google doc">google doc</Option>
                                  </Select>
                            </div>
                        </div>
                        <div style={{display: this.state.type === "1" ? 'flex': 'none', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>选择收件人邮箱
                            </div>
                            <div>
                                 <Select style={{width: 160}} value={this.state.target} disabled onChange={v => {
                                    this.setState({ target: v })
                                  }}>
                                     <Option value=""></Option>
                                  </Select>
                            </div>
                        </div>
                        <div style={{display: 'flex', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>最终目标URL
                            </div>
                            <div>
                                <Input.TextArea
                                    value={this.state.paths}
                                    onChange={(e) => this.setState({paths: e.target.value})}
                                    style={{width: 400, height: 200}}
                                    placeholder='请输入'
                                />
                            </div>
                        </div>
                        <div style={{display: 'flex', marginBottom: 24}}>
                            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                <span style={{color: '#D54941'}}>*</span>每个URL生成数量
                            </div>
                            <div>
                                <Input value={this.state.num}
                                    onChange={(e) => this.setState({num: e.target.value})} style={{width: 400}}
                                    placeholder='请输入'
                                />
                            </div>
                        </div>
                    </div>
                </Dialog>

                <Dialog
                    header="查看"
                    width={854}
                    height={566}
                    placement='center'
                    visible={this.state.checkVisible}
                    footer={false}
                    onClose={()=>{this.setState({checkVisible:false, currentRecord: null})}}
                >
                    <div style={{marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
                        {this.state.currentRecord && (
                            <>
                                <div style={{display: 'flex', marginBottom: 24}}>
                                    <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                        任务名称
                                    </div>
                                    <div>
                                        <Input
                                            value={this.state.currentRecord.workName}
                                            readOnly
                                            style={{width: 400}}
                                        />
                                    </div>
                                </div>
                                <div style={{display: 'flex', marginBottom: 24}}>
                                    <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                        目标URL
                                    </div>
                                    <div>
                                        <Input
                                            value={this.state.currentRecord.domain}
                                            readOnly
                                            style={{width: 400}}
                                        />
                                    </div>
                                </div>
                                <div style={{display: 'flex', marginBottom: 24}}>
                                    <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                        链接数
                                    </div>
                                    <div>
                                        <Input
                                            value={this.state.currentRecord.count}
                                            readOnly
                                            style={{width: 400}}
                                        />
                                    </div>
                                </div>
                                <div style={{display: 'flex', marginBottom: 24}}>
                                    <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                        创建时间
                                    </div>
                                    <div>
                                        <Input
                                            value={this.formatBeijingTime(this.state.currentRecord.latestCreateTime)}
                                            readOnly
                                            style={{width: 400}}
                                        />
                                    </div>
                                </div>
                                <div style={{display: 'flex', marginBottom: 24}}>
                                    <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                                        DNS列表
                                    </div>
                                    <div>
                                        <Input.TextArea
                                            value={this.state.currentRecord.dnsList.join('\n')}
                                            readOnly
                                            style={{width: 400, height: 200}}
                                        />
                                    </div>
                                </div>
                            </>
                        )}
                    </div>
                </Dialog>

            </ExternalAccountFilter></MyTranslate>
        )
    }
}

export default DnsRecord;
