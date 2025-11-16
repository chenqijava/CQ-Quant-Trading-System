import React, {Component} from 'react'
import {
  Icon,
  message,
  Upload,
  Input,
  Button,
} from 'antd'

const videoUploadProps = {
  name: 'file',
  multiple: false,
  action: '/api/consumer/res/upload/video',
  beforeUpload(file, fileList) {
    const isLt2M = file.size / 1024 / 1024 < 1;
    if (!isLt2M) {
      message.error('文件必须小于1MB!');
      return false;
    }
    let mp3 = /^.+\.mp4$/.test(file.name.toLowerCase());
    if (!mp3) {
      message.error('文件名不合法');
    }
    return mp3;
  }
};

class InputVideoMsg extends Component {

  constructor(props) {
    super(props);
    this.state = {
    }
  }

  async componentWillMount() {
  }

  onUploadChange = async(info) => {
    if(!info.file.status){
      return;
    }
    this.props.handleUploadChange(info.fileList);

    if(info.file.status === 'done'){
      if(info.file.response.code){
        message.success(`${info.file.name} 上传成功`);
      }else{
        message.error(info.file.response.message || `${info.file.name} 上传失败`);
        this.props.handleUploadChange([]);
      }
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} 上传失败`);
    }
  };

  render() {
    return (
      <div className="clearfix">
        {
          this.props.hasOwnProperty("videoName")?
          <Input style={{marginBottom: 8}} addonBefore="名称" placeholder="仅用于区分不同的视频" value={this.props.videoName} onChange={this.props.handleVideoNameChange}/>
          : null
        }
        <div>mp4格式，文件小于1MB</div>
        <Upload
          {...videoUploadProps}
          data={{service: "video"}}
          fileList={this.props.fileList}
          onChange={this.onUploadChange.bind(this)}
        >
          {this.props.fileList.length >= 1 ? null : (
              <Button>
                <Icon type="upload"/> 选择文件
              </Button>
          )}
        </Upload>
      </div>
    )
  }
}

export default InputVideoMsg;
