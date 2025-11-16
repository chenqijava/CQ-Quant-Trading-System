import React from 'react';
import ReactDOM from 'react-dom';
import SuccessDialog from './successDialog';
import ErrorDialog from './errorDialog';
import WarningDialog from './warningDialog';
import InfoDialog from './infoDialog';

let msgBox = null;

function _onCancel() {
    if (!!msgBox) {
      ReactDOM.unmountComponentAtNode(msgBox);
      msgBox = null
    }
  }

export default {
    success: async ({title, content, onOk, onCancel, onCancelTxt, onOkTxt, width=480}) => {
        _onCancel()

        msgBox = document.createElement('div');
        document.body.appendChild(msgBox);

        ReactDOM.render(
            <SuccessDialog
              width={width}
              title={title}
              visible={true}
              onCancel={onCancel ? () => {onCancel();_onCancel();}:undefined}
              onOk={onOk ? () => {onOk();_onCancel();}:undefined}
              onCancelTxt={onCancelTxt}
              onOkTxt={onOkTxt}
            >{content}</SuccessDialog>,
            msgBox
          );
    },
    warning: async ({title, content, onOk, onCancel, onCancelTxt, onOkTxt, width=480}) => {
        _onCancel()

        msgBox = document.createElement('div');
        document.body.appendChild(msgBox);

        ReactDOM.render(
            <WarningDialog
                width={width}
                title={title}
                visible={true}
                onCancel={onCancel ? () => {onCancel();_onCancel();}:undefined}
                onOk={onOk ? () => {onOk();_onCancel();}:undefined}
                onCancelTxt={onCancelTxt}
                onOkTxt={onOkTxt}
            >{content}</WarningDialog>,
            msgBox
          );
    },
    error: async ({title, content, onOk, onCancel, onCancelTxt, onOkTxt, width=480}) => {
        _onCancel()

        msgBox = document.createElement('div');
        document.body.appendChild(msgBox);

        ReactDOM.render(
            <ErrorDialog
                width={width}
                title={title}
                visible={true}
                onCancel={onCancel ? () => {onCancel();_onCancel();}:undefined}
                onOk={onOk ? () => {onOk();_onCancel();}:undefined}
                onCancelTxt={onCancelTxt}
                onOkTxt={onOkTxt}
            >{content}</ErrorDialog>,
            msgBox
          );
    },
    info: async ({title, content, onOk, onCancel, onCancelTxt, onOkTxt, width=480}) => {
      _onCancel()

      msgBox = document.createElement('div');
      document.body.appendChild(msgBox);

      ReactDOM.render(
          <InfoDialog
              width={width}
              title={title}
              visible={true}
              onCancel={onCancel ? () => {onCancel();_onCancel();}:undefined}
              onOk={onOk ? () => {onOk();_onCancel();}:undefined}
              onCancelTxt={onCancelTxt}
              onOkTxt={onOkTxt}
          >{content}</InfoDialog>,
          msgBox
        );
  }
}
