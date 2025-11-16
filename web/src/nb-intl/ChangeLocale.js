import {FormattedMessage} from 'react-intl';
import {connect} from "dva";
// import {
//   Dropdown, Menu
// } from 'antd'
import translate from 'images/img1/icons/translate.png'
import {Dropdown,Button} from 'tdesign-react'
import {ChevronDownIcon} from 'tdesign-icons-react'

import Style from './ChangeLocale.css'

export default connect(({locale}) => ({
  locale: locale.locale
}))(
    (props) => {
      const {
        locale,
        dispatch,
      } = props;

      const clickHandler = (key) => {
        console.log(key)
        dispatch({
          type: 'locale/changeLocale',
          locale: key.value, 
        })
      };
      

      // const menu = (
      //   <Menu onClick={onClick}>
      //     <Menu.Item key="zh">
      //       中文
      //     </Menu.Item>
      //     <Menu.Item key="en">
      //       English
      //     </Menu.Item>
      //   </Menu>
      // );

      const options = [
        {
          content: '简体中文',
          value: 'zh',
        },
        {
          content: 'English',
          value: 'en',
        },
      ];

      return <Dropdown style={{display: 'none'}} options={options} onClick={clickHandler}>
        <div style={{display: 'flex', alignItems: 'center', display: 'none'}}>
        <img className={Style.btn_img} src={translate} alt=""/>
        <span className={Style.btn_txt}>{locale == 'zh' ? '简体中文' : 'English'}</span>
        <ChevronDownIcon />
        {/* <Icon name="chevron-down" size="16" /> */}
        </div>
        {/* <Button variant="text" suffix={<Icon name="chevron-down" size="16" />}>
          {locale == 'zh' ? '简体中文' : 'English'}
        </Button> */}
      </Dropdown>
    }
)
