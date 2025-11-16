import React, {Component} from 'react'
import {Icon, message, Upload, Input, Modal} from 'antd'

function beforeUpload(file, fileList, regExp) {
  const isLt2M = file.size / 1024 / 1024 < 3;
  if (!isLt2M) {
    message.error('文件必须小于3MB!');
    return false
  }
  let jpg = regExp.test(file.name.toLowerCase());
  if (!jpg) {
    message.error('文件名不合法')
  }
  return jpg
}

const defaultUploadProps = {
  name: 'file',
  multiple: false,
  action: '/api/consumer/res/wxHeadUpload',
  beforeUpload(file, fileList) {
    return beforeUpload(file, fileList, /^.+\.(jpg|png|jpeg)$/);
  }
};

function getBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}

class InputImageMsg extends Component {

  constructor(props) {
    super(props);
    this.state = {
      value: null,
      previewVisible: false,
      previewImage: '',
    }
  }

  async componentWillMount() {
  }

  handlePreview = async (file) => {
    if (!file.url && !file.preview) {
      file.preview = await getBase64(file.originFileObj);
    }

    this.setState({
      previewImage: file.url || file.preview,
      previewVisible: true
    });
  };

  onUploadChange = async (info) => {
    if (typeof this.props.onUploadChange == 'function') this.props.onUploadChange(info);
    if (!info.file.status) {
      return;
    }
    await this.setState({value: info.fileList});
    this.triggerChange(info.fileList);

    if (info.file.status === 'done') {
      if (typeof this.props.handleUploadDone == 'function') {
        this.props.handleUploadDone(info)
      }
      message.success(`${info.file.name} 上传成功`);
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} 上传失败`);
    }
  };

  triggerChange = fileList => {
    const {onChange, handleUploadChange} = this.props;
    if (onChange) {
      onChange(fileList);
    }
    if (typeof handleUploadChange == 'function') {
      handleUploadChange(fileList)
    }
  };

  render() {
    const {
      previewVisible,
      previewImage,
    } = this.state;

    const {
      value,
      maxLength,
      desc,
      uploadProps,
      disabled,
      style=''
    } = {
      ...this.state,
      ...this.props,
    };
    console.log("style:",style)
    if (uploadProps) {
      if (!uploadProps.action && uploadProps.uploadFileType) {
        uploadProps.action = `/api/consumer/res/upload/${uploadProps.uploadFileType}`
      }
      if (!uploadProps.beforeUpload && uploadProps.beforeUploadRegExp) {
        uploadProps.beforeUpload = (file, fileList) => {
          return beforeUpload(file, fileList, uploadProps.beforeUploadRegExp);
        }
      }
    }

    const uploadButton = (
        <div>
          <Icon type="plus"/>
          <div className="ant-upload-text">Upload</div>
        </div>
    );

    let {hidePlus} = {
      hidePlus: (value || []).length >= (maxLength || 1),
      ...this.props,
    };
    console.log("style:",style)

    return (
        <div className="clearfix" style={style ? style:{minHeight: '136px'}}>
          <div>请选择jpg,png,jpeg格式的文件上传, 文件小于3MB</div>
          <Upload
              disabled={disabled}
              {...defaultUploadProps}
              {...uploadProps}
              listType="picture-card"
              fileList={value}
              onPreview={this.handlePreview.bind(this)}
              onChange={this.onUploadChange.bind(this)}
          >
            {hidePlus ? null : uploadButton}
          </Upload>
          <Modal visible={previewVisible} footer={null}
                 onCancel={() => {
                   this.setState({previewVisible: false})
                 }}>
            <img alt="example" style={{width: '100%'}} src={previewImage}/>
          </Modal>
        </div>
    )
  }
}

export default InputImageMsg;
