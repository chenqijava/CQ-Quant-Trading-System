import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link, withRouter } from 'react-router-dom'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import { connect } from 'dva'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  Modal,
  Card,
  Avatar,
  DatePicker,
  Select,
  Collapse,
  Alert,
} from 'antd'
import axios from 'axios'
import Style from './index.css'
import customerImage from '../../images/img1/customer.png'
import operatorImage from '../../images/img1/operator.png'
import accountList from '../../images/img1/accountList.png'
import advertisement from '../../images/img1/advertisement.png'
import automaticFeed from '../../images/img1/automaticFeed.png'
import chatmember from '../../images/img1/chatmember.png'
import chatmonitor from '../../images/img1/chatmonitor.png'
import dataRobot from '../../images/img1/dataRobot.png'
import fireChatroom from '../../images/img1/fireChatroom.png'
import joinSendMsg from '../../images/img1/joinSendMsg.png'
import material from '../../images/img1/material.png'
import monthFriend from '../../images/img1/monthFriend.png'
import sendMsg from '../../images/img1/sendMsg.png'
import todayFriend from '../../images/img1/todayFriend.png'
import weekFriend from '../../images/img1/weekFriend.png'
import yestodayFriend from '../../images/img1/yestodayFriend.png'
import indexImage from '../../images/index.jpg'
import { Breadcrumb, Swiper, Button as TButton, ImageViewer, Image, Space, Row, Col } from 'tdesign-react';
import { Pie, Line } from '@ant-design/plots';
import TimerWrapper from "../../components/TimerWrapper";

const { BreadcrumbItem } = Breadcrumb;
const { SwiperItem } = Swiper;
const { RangePicker } = DatePicker;
const { Panel } = Collapse;

const COLOR_PLATE_10 = [
  '#5B8FF9',
  '#5AD8A6',
  '#5D7092',
  '#F6BD16',
  '#E8684A',
  '#6DC8EC',
  '#9270CA',
  '#FF9D4D',
  '#269A99',
  '#FF99C3',
];

const menus = [
  { imageSrc: accountList, name: '账号列表', url: '/cloud/account/account' },
  { imageSrc: sendMsg, name: '好友群发', url: '/cloud/account/stranger/sendMsg' },
  { imageSrc: joinSendMsg, name: '群聊群发', url: '/cloud/account/chatroom/joinAndSendMsg' },
  { imageSrc: fireChatroom, name: ' 炒群 ', url: '/cloud/account/chatroom/sendScene' },
  { imageSrc: automaticFeed, name: '自动养号', url: '/cloud/account/friend/yangAuto' },
  { imageSrc: chatmonitor, name: '群聊监听', url: '/cloud/account/chatroom/supervise' },
  { imageSrc: advertisement, name: '广告号', url: '/cloud/account/chatroom/advertisementAccount' },
  { imageSrc: material, name: '素材库', url: '/cloud/account/material/nlist' },
  { imageSrc: chatmember, name: '群成员采集', url: '/cloud/account/chatroom/GrabIDs' },
  { imageSrc: dataRobot, name: '数据机器人', url: '/cloud/account/datacenter/ai' },
]


class Index extends Component {

  constructor(props) {
    super(props);
    this.state = {
      totalData: {},
      version: {},
      balance: 0,
      total: 0,
      binds: 0,
      unbinds: 0,
      deviceTotal: 0, //设备总数
      toDeviceTotal: 0, //即将到期
      deadDeviceTotal: 0, //已到期
      allocatedTotal: 0, //已分配
      notAllocatedTotal: 0, //未分配
      accountTotal: 0, //账号总数
      onlineAccountTotal: 0,//在线账号数
      unlineAccountTotal: 0,//离线账号数
      bannedAccountTotal: 0,//封禁账号数
      indexplacards: [], //首页海报
      accID: '',//账号id
      groupID: '',//分组id
      createTime: '',//时间区间
      days: 7, //近7天
      todayFriends: {}, //今日加粉
      yesTodayFriends: {}, //昨日加粉
      weekFriends: {}, //本周加粉
      monthFriends: {}, //本月加粉
      //accountInfos: [], //所有账号
      accountGroupList: [], //分组
      pieConfig: {
        appendPadding: 10,
        data: [],
        angleField: 'value',
        colorField: 'type',
        radius: 0.8,
        innerRadius: 0.6,
        label: {
          type: 'inner',
          offset: '-50%',
          content: '{value}',
          style: {
            textAlign: 'center',
            fontSize: 14,
          },
        },
        interactions: [
          {
            type: 'element-selected',
          },
          {
            type: 'element-active',
          },
        ],
        statistic: {
          title: false,
          content: {
            style: {
              whiteSpace: 'pre-wrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            },
            content: 'AntV\nG2Plot',
          },
        },
      }, //饼图配置
      lineConfig: {
        data: [],
        xField: 'day',
        yField: 'value',
        seriesField: 'category',
        // yAxis: {
        //   label: {
        //     // 数值格式化为千分位
        //     formatter: (v) => `${v}`.replace(/\d{1,3}(?=(\d{3})+$)/g, (s) => `${s},`),
        //   },
        // },
        color: COLOR_PLATE_10,
        // point: {
        //   shape: ({ category }) => {
        //     return category === 'Gas fuel' ? 'square' : 'circle';
        //   },
        //   style: ({ year }) => {
        //     return {
        //       r: Number(year) % 4 ? 0 : 3, // 4 个数据示一个点标记
        //     };
        //   },
        // },
      }, //折线图表配置
    }
    this.filters = {};
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload();
    this.addFriendTendency(30)
    this.loadData()
  }

  async loadData() {
    let res = await axios.post(`/api/googleStudio/canUseCount`, {});
    this.setState({
      totalData: res.data.data
    })
  }


  chargeOpen = async () => {
    // window.open('https://t.me/Messi0831', '_blank');
    // window.location.href='/cloud/user/payment';
    let keys = [...this.props.openKeys, 'balanceMenu']
    this.props.dispatch({ type: 'user/openKeys', openKeys: keys });
    this.props.history.push('/cloud/user/payment')
  };

  async operationManual() {
    window.open('https://malls-organization-1.gitbook.io/tnt-rcs', '_blank');
  }

  async contactCustomer() {
    window.open('https://t.me/QShan999', '_blank');
  }

  async addFriendTendency(days) {
    let data = [];
    // let filter = {};

    let res = await axios.post(`/api/aiStatistics/${days}/1`, {})
    if (res.data.code === 1) {
      res.data.data.map(e => {
        data.push({
          day: e.createTime.split('T')[0],
          value: e.callNum,
          category: '调用次数'
        })
        data.push({
          day: e.createTime.split('T')[0],
          value: e.successNum,
          category: '成功次数'
        })
        data.push({
          day: e.createTime.split('T')[0],
          value: e.accountNum || 0,
          category: '账号数量'
        })
      })
    }

    data.sort((a, b) => {
      return new Date(a.day) - new Date(b.day);
    })
    // const data = [
    //   { year: '2010', value: 3, type: 'A' },
    //   { year: '2011', value: 4, type: 'A' },
    //   { year: '2012', value: 3.5, type: 'A' },
    //   { year: '2010', value: 2, type: 'B' },
    //   { year: '2011', value: 5, type: 'B' },
    //   { year: '2012', value: 6, type: 'B' },
    // ];

    const lineConfig = {
      data,
      xField: 'day',
      yField: 'value',
      seriesField: 'category', // 👈 多条折线的关键
      yAxis: {
        label: {
          formatter: (v) => `${v}`,
        },
      },
      legend: {
        position: 'top',
      },
      smooth: true,
      point: {
        size: 5,
        shape: 'circle',
      },
    };

    // let lineConfig = {
    //   data: data,
    //   xField: 'day',
    //   yField: 'value',
    //   seriesField: 'category',
    //   // yAxis: {
    //   //   label: {
    //   //     // 数值格式化为千分位
    //   //     formatter: (v) => `${v}`.replace(/\d{1,3}(?=(\d{3})+$)/g, (s) => `${s},`),
    //   //   },
    //   // },
    //   color: COLOR_PLATE_10,
    //   // point: {
    //   //   shape: ({ category }) => {
    //   //     return category === 'Gas fuel' ? 'square' : 'circle';
    //   //   },
    //   //   style: ({ year }) => {
    //   //     return {
    //   //       r: Number(year) % 4 ? 0 : 3, // 4 个数据示一个点标记
    //   //     };
    //   //   },
    //   // },
    // }

    this.setState({ lineConfig: lineConfig })
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load()
  }

  async load() {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res1 = await axios.get(`/api/consumer/user/balance`);
    let res2 = { data: {} } // await axios.get(`/api/consumer/vps/bindVpsCount`);
    this.setState({ loading: false, balance: res1.data.data, ...res1.data, ...res2.data });
  }

  render() {
    const marginBottom = '10px';
    return (<div style={{ background: '#f0fbff' }}>
      <Breadcrumb>
        <BreadcrumbItem>用户中心</BreadcrumbItem>
        <BreadcrumbItem>个人中心</BreadcrumbItem>
      </Breadcrumb>

      <div style={{ padding: '20px' }}>
        <Row>
          <Col xs={8} sm={8} md={8} lg={8} xl={8}>
            <div style={{ height: '300px', background: '#fff', borderRadius: '12px' }}>
              <Swiper duration={300} interval={5000}>
                {this.state.indexplacards.map((item) => (
                  <SwiperItem>
                    <Image src={item} fit="fill" style={{ height: '300px', borderRadius: '12px' }} />
                  </SwiperItem>
                ))}
              </Swiper>
            </div>
          </Col>
          <Col xs={4} sm={4} md={4} lg={4} xl={4} style={{ paddingLeft: '10px' }}>
            <div style={{ height: '300px', background: '#fff', borderRadius: '12px' }}>
              <Row>
                <Col xs={12} sm={12} md={12} lg={12} xl={12}>
                  <div style={{ paddingTop: '15px', paddingLeft: '24px', paddingRight: '24px' }}>
                    <div style={{ border: '1px solid #E7E7E7', borderRadius: '8px' }}>
                      <div style={{ display: 'flex', marginBottom: '12px' }}>
                        <span style={{
                          color: '#3978f7',
                          width: '100%',
                          display: 'block',
                          marginLeft: '24px',
                          marginTop: '12px',
                          fontSize: '12px',
                          fontWeight: 400,
                          lineHeight: '22px'
                        }}>当前余额</span>
                      </div>
                      <div style={{ marginBottom: '12px' }}>
                        <span style={{
                          fontSize: '30px',
                          fontWeight: '600',
                          marginLeft: '24px',
                          color: '#000',
                          fontStyle: 'normal',
                          lineHeight: '22px'
                        }}>${this.state.balance}</span>
                      </div>
                      {/* <TButton style={{
                        width: '89%',
                        marginLeft: '24px',
                        marginBottom: '12px',
                        background: 'var(--Color, #3978F7)',
                        border: 'none'
                      }} className="search-query-btn" onClick={this.chargeOpen.bind(this)}>立即充值</TButton> */}
                    </div>
                  </div>
                </Col>
              </Row>
              <Row>
                <div style={{ padding: '10px 24px 24px', width: '100%', display: 'flex' }}>
                  <Col xs={6} sm={6} md={6} lg={6} xl={6} style={{ width: '48%' }}>
                    <div style={{ border: '1px solid #E7E7E7', borderRadius: '8px', textAlign: 'center' }}>
                      <Space direction="vertical" align="start"
                        style={{ marginTop: '15px', marginBottom: '15px', marginLeft: '10px' }}>
                        <Image
                          src={customerImage}
                          fit="contain"
                          style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: 'var(--td-radius-medium)',
                            backgroundColor: '#fff',
                            cursor: 'pointer'
                          }}
                          onClick={() => this.contactCustomer()}
                        />
                        <div style={{
                          fontWeight: '400',
                          fontSize: '16px',
                          marginTop: '-10px',
                          cursor: 'pointer',
                          lineHeight: '22px',
                          marginLeft: '-10px'
                        }} onClick={() => this.contactCustomer()}>联系客服
                        </div>
                        <div style={{
                          fontWeight: '400',
                          fontSize: '12px',
                          lineHeight: '22px',
                          color: 'var(--Gray-Gray7, #8B8B8B)',
                          marginTop: '-10px',
                          cursor: 'pointer',
                          marginLeft: '-3px'
                        }} onClick={() => this.contactCustomer()}>立即咨询
                        </div>
                      </Space>
                    </div>
                  </Col>
                  <Col xs={6} sm={6} md={6} lg={6} xl={6} style={{ width: '48%', paddingLeft: '10px' }}>
                    <div style={{ border: '1px solid #E7E7E7', borderRadius: '8px', textAlign: 'center' }}>
                      <Space direction="vertical" align="start"
                        style={{ marginTop: '15px', marginBottom: '15px', marginLeft: '10px' }}>
                        <Image
                          src={operatorImage}
                          fit="contain"
                          style={{
                            width: '40px',
                            height: '40px',
                            borderRadius: 'var(--td-radius-medium)',
                            backgroundColor: '#fff',
                            cursor: 'pointer'
                          }}
                          onClick={() => this.operationManual()}
                        />
                        <div style={{
                          fontWeight: '400',
                          fontSize: '16px',
                          lineHeight: '22px',
                          marginTop: '-10px',
                          cursor: 'pointer',
                          marginLeft: '-10px'
                        }} onClick={() => this.operationManual()}>操作手册
                        </div>
                        <div style={{
                          fontWeight: '400',
                          fontSize: '12px',
                          lineHeight: '22px',
                          marginTop: '-10px',
                          color: 'var(--Gray-Gray7, #8B8B8B)',
                          cursor: 'pointer',
                          marginLeft: '-3px'
                        }} onClick={() => this.operationManual()}>使用指南
                        </div>
                      </Space>
                    </div>
                  </Col>
                </div>
              </Row>
            </div>
          </Col>
        </Row>
        {/* <Row style={{paddingTop: '20px'}}>
          <Col xs={9} sm={9} md={9} lg={9} xl={9}>
            <div style={{borderRadius: '12px', background: '#fff', padding: '24px 40px 40px 40px'}}>
              <Row>
                <Col xs={12} sm={12} md={12} lg={12} xl={12}>
                  <div style={{display: 'flex'}}>
                    <div style={{marginBottom: '28px'}}><span style={{
                      color: '#000',
                      fontSize: '16px',
                      fontWeight: '600',
                      lineHeight: '22px'
                    }}>设备使用情况</span></div>
                  </div>
                </Col>
              </Row>
              <Row>
                <Col xs={11} sm={12} md={12} lg={12} xl={12}>
                  <div style={{display: 'flex'}}>
                    <div style={{borderRadius: '12px', background: '#52c41a14', width: '19%'}}>
                      <div style={{padding: '24px 24px 24px 24px'}}>
                        <div><span style={{
                          color: '#2BA471',
                          fontSize: '16px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>设备总数</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#000',
                          fontSize: '24px',
                          fontWeight: '600',
                          lineHeight: '22px'
                        }}>{this.state.deviceTotal}</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#4B4B4B',
                          fontSize: '12px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>所有设备</span></div>
                      </div>
                    </div>

                    <div style={{borderRadius: '12px', background: '#FEF3C7', width: '19%', marginLeft: '24px'}}>
                      <div style={{padding: '24px 24px 24px 24px'}}>
                        <div><span style={{fontSize: '16px', color: '#D97706'}}>即将到期</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#000',
                          fontSize: '24px',
                          fontWeight: '600',
                          lineHeight: '22px'
                        }}>{this.state.toDeviceTotal}</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#4B4B4B',
                          fontSize: '12px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>5天内到期</span></div>
                      </div>
                    </div>

                    <div style={{borderRadius: '12px', background: '#FFF0ED', width: '19%', marginLeft: '24px'}}>
                      <div style={{padding: '24px 24px 24px 24px'}}>
                        <div><span style={{fontSize: '16px', color: '#D54941'}}>已到期</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#000',
                          fontSize: '24px',
                          fontWeight: '600',
                          lineHeight: '22px'
                        }}>{this.state.deadDeviceTotal}</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#4B4B4B',
                          fontSize: '12px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>需要续费</span></div>
                      </div>
                    </div>

                    <div style={{borderRadius: '12px', background: '#DBEAFE', width: '19%', marginLeft: '24px'}}>
                      <div style={{padding: '24px 24px 24px 24px'}}>
                        <div><span style={{fontSize: '16px', color: '#3978F7'}}>已分配</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#000',
                          fontSize: '24px',
                          fontWeight: '600',
                          lineHeight: '22px'
                        }}>{this.state.allocatedTotal}</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#4B4B4B',
                          fontSize: '12px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>已使用设备</span></div>
                      </div>
                    </div>

                    <div style={{borderRadius: '12px', background: '#EEEBFE', width: '19%', marginLeft: '24px'}}>
                      <div style={{padding: '24px 24px 24px 24px'}}>
                        <div><span style={{fontSize: '16px', color: '#7C3AED'}}>未分配</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#000',
                          fontSize: '24px',
                          fontWeight: '600',
                          lineHeight: '22px'
                        }}>{this.state.notAllocatedTotal}</span></div>
                        <div style={{marginTop: '10px'}}><span style={{
                          color: '#4B4B4B',
                          fontSize: '12px',
                          fontWeight: '400',
                          lineHeight: '22px'
                        }}>可分配设备</span></div>
                      </div>
                    </div>
                  </div>
                </Col>
              </Row>
            </div>
          </Col>
          <Col xs={3} sm={3} md={3} lg={3} xl={3} style={{paddingLeft: '10px'}}>
            <div style={{
              borderRadius: '12px',
              background: '#fff',
              paddingTop: '24px',
              paddingBottom: '20px',
              height: '254px'
            }}>
              <Row>
                <Col xs={12} sm={12} md={12} lg={12} xl={12}>
                  <div style={{display: 'flex'}}>
                    <div style={{marginBottom: '28px', marginLeft: '28px'}}><span style={{
                      color: '#000',
                      fontSize: '16px',
                      fontWeight: '600',
                      lineHeight: '22px'
                    }}>账号使用情况</span></div>
                  </div>
                </Col>
              </Row>
              <Row>
                <Col xs={12} sm={12} md={12} lg={12} xl={12}>
                  <div style={{width: '260px', marginLeft: '10px'}}>
                    <Pie {...this.state.pieConfig} style={{marginTop: '-123px'}}/>
                  </div>
                </Col>
              </Row>
            </div>
          </Col>
        </Row> */}

        <div style={{ display: 'flex', lineHeight: '40px', color: '#000', fontSize: '16px', fontWeight: '600' }}>最近10分钟内成功使用key数量：{this.state.totalData.keyCount}   &nbsp;今天剩余次数(按一个账号1500次)：{this.state.totalData.restCount}   
        </div>

        <div style={{ width: '100%', padding: '20px', backgroundColor: '#fff', marginTop: '20px' }}>
          <h2>
            AI最近30天的调用次数
          </h2>
          <div style={{ height: '400px' }}>
            <Line {...this.state.lineConfig} />
          </div>
        </div>
      </div>
    </div>)
  }
}

export default connect(({ user }) => ({
  userID: user.info.userID,
  name: user.info.name,
  openKeys: user.openKeys,
}))(TimerWrapper(injectIntl(withRouter(Index))))

