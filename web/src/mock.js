import Mock from 'mockjs'

Mock.setup({
  timeout: 300
})

Mock.mock('/api/common/user/customerLogin', 'post', () => {
  return {code: 1}
})

Mock.mock('/api/customer/loadWXGroups', 'post', () =>{
  let accountGroupList = [];
  for(let i = 0; i < Mock.Random.integer(10, 20); i++){
    accountGroupList.push({
      "_id": "group-" + i,
      "nickname": "组" + i,
      "smallHeadPic": "https://c-ssl.duitang.com/uploads/item/201705/08/20170508225328_fwyFQ.thumb.700_0.jpeg",
      "unreadMsgCount": Mock.Random.integer(10, 1000),
      "latestMsgTime": 1567048828641,
      "customerNewFriendCount": Mock.Random.natural(0, 1000),
    });
  }
  return {
    code: 1,
    accountGroupList
  }
});

Mock.mock('/api/customer/loadWXAccounts', 'post', ({body}) => {
  body = JSON.parse(body)
  let wxAccountList = []
  for (let i = 0; i < body.pageSize; i++) {
    wxAccountList.push({
      "_id": "666" + Mock.Random.natural(),
      "accID": "wxid_" + Mock.Random.natural(),
      "smallHeadPic": "https://c-ssl.duitang.com/uploads/item/201904/26/20190426091940_CGZQC.thumb.100_100_c.jpeg",
      "nickname": "东风路" + i,
      "unreadMsgCount": Mock.Random.natural(0, 1500),
      "latestMsgTime": 1567048828641,
      "followed": true,
      "customerNewFriendCount": Mock.Random.natural(0, 1000),
    })
  }
  return {
    code: 1,
    wxAccountList
  }
})

// 根据账号，获取账号基本信息
Mock.mock('/api/customer/loadWXAccountSingle', 'post', ({body}) => {
  body = JSON.parse(body)
  return {
    code: 1,
    account: {
      "_id": "666" + Mock.Random.natural(),
      "accID": body.accID,
      "smallHeadPic": "https://c-ssl.duitang.com/uploads/item/201904/26/20190426091940_CGZQC.thumb.100_100_c.jpeg",
      "nickname": "东风路single",
      "unreadMsgCount": Mock.Random.natural(0, 1500),
      "latestMsgTime": 1567048828641,
      "followed": true
    }
  }
})

Mock.mock('/api/customer/loadWXAccountFriends', 'post', ({body}) => {
  body = JSON.parse(body)
  let wxFriendList = []
  for (let i = 0; i < 30; i++) {
    wxFriendList.push({
      "_id": "666" + Mock.Random.natural(),
      "accID": "wxid_" + Mock.Random.natural(),
      "ownerAccID": body.accID,
      "smallHeadPic": "https://c-ssl.duitang.com/uploads/item/201902/17/20190217144312_4s5Cd.thumb.700_0.jpeg",
      "nickname": "东" + i,
      "unreadMsgCount": Mock.Random.natural(0, 10),
      "latestMsgTime": 1567048828641,
      "followed": false,
      "newFriend": true,
    })
  }
  return {
    code: 1,
    wxFriendList
  }
})

Mock.mock('/api/customer/markReadMsgNum', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/followWXAccount', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/unfollowWXAccount', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/followWXFriend', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/unfollowWXFriend', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/loadChatRecord', 'post', ({body}) => {
  let msg = []
  for (let i=0;i<5;i++) {
    let ownerAccID = "wxid_" + Mock.Random.natural()
    msg.push({
      "_id": "666" + Mock.Random.natural(),
      "ownerAccID": ownerAccID,
      "fromWxID": Mock.Random.boolean() ? ownerAccID:"wxid2_" + Mock.Random.natural(),
      "toWxID": "wxid2_" + Mock.Random.natural(),
      "msgType": 1,
      "content": "你好，Hello World!",
      "createTime": Math.floor(new Date().getTime()/1000),
      "syncTime": Math.floor(new Date().getTime()/1000),
      "msgStatus": Mock.Random.natural(0, 2),
      "msgError": "这是错误信息",
      "sendAtTime": Math.floor(new Date().getTime()/1000) + Mock.Random.natural(-100, 100)
    })
  }
  for (let i=0;i<5;i++) {
    let ownerAccID = "wxid_" + Mock.Random.natural()
    msg.push({
      "_id": "666" + Mock.Random.natural(),
      "ownerAccID": ownerAccID,
      "fromWxID": Mock.Random.boolean() ? ownerAccID:"wxid2_" + Mock.Random.natural(),
      "toWxID": "wxid2_" + Mock.Random.natural(),
      "msgType": 3,
      "imgBuf": {
        buffer: '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'
      },
      "createTime": Math.floor(new Date().getTime()/1000),
      "syncTime": Math.floor(new Date().getTime()/1000),
      "msgStatus": Mock.Random.natural(0, 2),
      "msgErrorCode": -44,
      "msgError": "对方已删除自己",
      "sendAtTime": Math.floor(new Date().getTime()/1000) + Mock.Random.natural(-100, 100)
    })
  }
  return {
    code: 1,
    chatList: msg
  }
})

Mock.mock('/api/customer/sendMessage', 'post', ({body}) => {
  return {
    code: 1,
    _id: "777" + Mock.Random.natural()
  }
})

Mock.mock('/api/customer/deleteTimingMessage', 'post', ({body}) => {
  // 删除消息
  return {
    code: 1,
    message: ''
  }
})

Mock.mock('/api/customer/revokeMessage', 'post', ({body}) => {
  // 删除消息
  return {
    code: 1,
    message: ''
  }
})

Mock.mock('/api/customer/loadWXAccountFriendInfo', 'post', ({body}) => {
  body = JSON.parse(body)
  let data = [
    {
      name: "头像",
      value: "https://c-ssl.duitang.com/uploads/item/201801/25/20180125173013_sveub.thumb.700_0.jpg"
    },
    {
      name: "昵称",
      value: "微风和煦"
    },
    {
      name: "性别",
      value: "1"
    },
    {
      name: "地区",
      value: ['Henan', 'Nanyang']
    }
  ];
  return {
    code: 1,
    data
  }
})

Mock.mock('/api/customer/loadWXAccountFriendCircle', 'post', ({body}) => {
  body = JSON.parse(body);
  let result = {};
  let circle = {mmret: -1, msg: "无朋友圈信息"}
  return {
    code: 1,
    circle
  }
})

Mock.mock('/api/customer/loadAllFollowedWXAccountFriends', 'post', ({body}) => {
  body = JSON.parse(body)
  let wxFriendList = []
  for (let i = 0; i < 3; i++) {
    wxFriendList.push({
      "_id": "666" + Mock.Random.natural(),
      "accID": "wxid_" + Mock.Random.natural(),
      "ownerAccID": "wxid2_" + Mock.Random.natural(),
      "ownerGroupID": "group-" + i,
      "smallHeadPic": "https://c-ssl.duitang.com/uploads/item/201801/25/20180125173013_sveub.thumb.700_0.jpg",
      "nickname": "历史的" + i,
      "unreadMsgCount": Mock.Random.natural(0, 10),
      "latestMsgTime": 1567048828641,
      "followed": true,
    })
  }
  return {
    code: 1,
    wxFriendList
  }
})

Mock.mock('/api/customer/delWxAccountFriend', 'post', ({body}) => {
  return {
    code: 1
  }
})

Mock.mock('/api/customer/resetAccountMsgNum', 'post', ({body}) => {
  return {
    code: 1
  }
})

// 获取自己子任务的信息
Mock.mock(/\/jifq\/getSubTaskInfo.+/, 'get', () => {
  return {
    code: 0
  }
})

// 换绑手机号
Mock.mock('/api/account/editAccountMobile', 'post', () => {
  return {
    code: 1,
    message: "mock error"
  }
})


Mock.mock(/\/api\/consumer\/friendMsg\/getAddFriendMsg\/.+/, 'get', () => {
  return {
    code: 1,
    data: [{
      _id: Mock.Random.natural(),
      fromWxID: "wxid_" + Mock.Random.natural(),
      fromNickname: "nickname"+ Mock.Random.natural(),
    }],
  }
})

Mock.mock('/api/consumer/account/deleteContactBatch', 'post', () => {
  return {
    code: 1,
  }
})
Mock.mock('/api/consumer/account/verifyContactBatch', 'post', () => {
  return {
    code: 1,
  }
})

