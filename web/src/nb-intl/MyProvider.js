import {connect} from "dva";
import {
  LocaleProvider,
  ConfigProvider,
} from 'antd'

import { IntlProvider, FormattedMessage } from 'react-intl';
import messages from './messages'

export default connect(({locale}) => ({
  locale: locale.locale
}))(({children, locale, dispatch}) => {
  return <IntlProvider locale={locale} messages={messages.ls[locale]}>
    <LocaleProvider locale={messages.lps[locale]}>
      {children}
    </LocaleProvider>
  </IntlProvider>
});
