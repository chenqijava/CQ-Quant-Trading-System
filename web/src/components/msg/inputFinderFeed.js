import React, {Component} from 'react'
import {Icon, message, Upload, Input, Alert} from 'antd'
import FinderfeedIntro from "../../images/finderfeed_intro.png";
const {TextArea} = Input;

class inputFinderFeed extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showType: this.props.showType || 'Card',
    }
  }

  async componentWillMount() {
  }

  async onchangeByTextArea(e) {
    try{
      this.onchange(e.target.value.split('\n').map(str => JSON.parse(str)));
    }catch (err) {
      this.onchange(e.target.value);
    }
  }

  async onchange(finderFeedDatas) {
    this.props.handleChange(finderFeedDatas);
  }

  render() {
    let finderFeedDatas = typeof this.props.finderFeedDatas === 'object' && this.props.finderFeedDatas.length > 0 ? this.props.finderFeedDatas : [];
    let finderFeedData = finderFeedDatas[0] || {};
    let media = (finderFeedData.media||[])[0]||{};
    let finderFeedDatasStr = typeof this.props.finderFeedDatas === 'string' ? this.props.finderFeedDatas : finderFeedDatas.map(ffd => JSON.stringify(ffd)).join('\r\n');
    return (
      <div className="clearfix">
        {
          this.props.showTips?
            <Alert message={<img src={FinderfeedIntro} alt=""/>}
            description={<div><span>卡片消息各个部分数据如上图所示</span><br/><b>其中链接为必填项</b><span>，其他项选填，为空则图中对应部分内容可能为空</span></div>}
            type="info"
          />
          : null
        }
        {
          this.props.showCardName?
            <Input addonBefore="名称" placeholder="仅用于区分不同的卡片" value={this.props.cardName} onChange={this.onchangeByOne.bind(this, 'cardName')}/>
            : null
        }
        {
          this.state.showType === 'Card' ? [
            <TextArea placeholder={"精聊界面复制的视频号参数"} rows={3} value={finderFeedDatasStr} onChange={this.onchangeByTextArea.bind(this)}/>
          ].concat([{
            key: 'objectId',
            addonBefore: '视频id',
            placeholder: '用于账号跳转视频界面',
          }, {
            key: 'objectNonceId',
            addonBefore: '视频分享id',
            placeholder: '',
          }, {
            key: 'nickname',
            addonBefore: '视频号昵称',
            placeholder: '',
          }, {
            key: 'avatar',
            addonBefore: '视频号头像',
            placeholder: '',
          }, {
            key: 'desc',
            addonBefore: '视频描述',
            placeholder: '',
          }].map(input =>
            <Input style={{marginTop: 8}} {...input} value={finderFeedData[input.key]} onChange={(e) => {
              this.onchange([{...finderFeedData,[input.key]: e.target.value}])
            }}/>
          )).concat([{
            key: 'url',
            addonBefore: '视频链接',
            placeholder: '查看视频的地址(仅用于本系统查看视频，与应用跳转的视频不相同)',
          }, {
            key: 'thumbUrl',
            addonBefore: '视频封面',
            placeholder: '视频封面的URL',}].map(input => <Input style={{marginTop: 8}} {...input} value={media[input.key]} onChange={(e) => {
            this.onchange([{...finderFeedData,media:[{...media,[input.key]:e.target.value}]}])
          }}/>)) : null
        }
        {this.state.showType === 'TextArea' ? <TextArea placeholder={"精聊界面复制的视频号参数，一行是一个视频号"} rows={6} value={finderFeedDatasStr} onChange={this.onchangeByTextArea.bind(this)}/> : null}
      </div>
    )
  }
}

export default inputFinderFeed;
