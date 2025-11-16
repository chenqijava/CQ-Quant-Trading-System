//  abbr后的注释是该国家可能用到的其他缩写
const area = [
  {
    "label_en_US": "Andorra",
    "label_zh_CN": "安道尔共和国",
    "abbr": "AD",
    "value": 376,
    "children": []
  },
  {
    "label_en_US": "The United Arab Emirates",
    "label_zh_CN": "阿拉伯联合酋长国",
    "abbr": "AE",
    "value": 971,
    "children": []
  },
  {
    "label_en_US": "Afghanistan",
    "label_zh_CN": "阿富汗",
    "abbr": "AF",
    "value": 93,
    "children": []
  },
  {
    "label_en_US": "Antigua and Barbuda",
    "label_zh_CN": "安提瓜和巴布达",
    "abbr": "AG",
    "value": 1268,
    "children": []
  },
  {
    "label_en_US": "Anguilla",
    "label_zh_CN": "安圭拉岛",
    "abbr": "AI",
    "value": 1264,
    "children": []
  },
  {
    "label_en_US": "Albania",
    "label_zh_CN": "阿尔巴尼亚",
    "abbr": "AL",
    "value": 355,
    "children": []
  },
  {
    "label_en_US": "Armenia",
    "label_zh_CN": "亚美尼亚",
    "abbr": "AM",
    "value": 374,
    "children": []
  },
  // 阿森松 Ascension 247
  {
    "label_en_US": "Angola",
    "label_zh_CN": "安哥拉",
    "abbr": "AO",
    "value": 244,
    "children": []
  },
  {
    "label_en_US": "Argentina",
    "label_zh_CN": "阿根廷",
    "abbr": "AR",
    "value": 54,
    "children": []
  },
  {
    "label_en_US": "Austria",
    "label_zh_CN": "奥地利",
    "abbr": "AT",
    "value": 43,
    "children": []
  },
  {
    "label_en_US": "Australia",
    "label_zh_CN": "澳大利亚",
    "abbr": "AU",
    "value": 61,
    "children": []
  },
  {
    "label_en_US": "Azerbaijan",
    "label_zh_CN": "阿塞拜疆",
    "abbr": "AZ",
    "value": 994,
    "children": []
  },
  {
    "label_en_US": "Barbados",
    "label_zh_CN": "巴巴多斯",
    "abbr": "BB",
    "value": 1246,
    "children": []
  },
  {
    "label_en_US": "Bangladesh",
    "label_zh_CN": "孟加拉国",
    "abbr": "BD",
    "value": 880,
    "children": []
  },
  {
    "label_en_US": "Belgium",
    "label_zh_CN": "比利时",
    "abbr": "BE",
    "value": 32,
    "children": []
  },
  {
    "label_en_US": "Burkina-faso",
    "label_zh_CN": "布基纳法索",
    "abbr": "BF",
    "value": 226,
    "children": []
  },
  {
    "label_en_US": "Bulgaria",
    "label_zh_CN": "保加利亚",
    "abbr": "BG",
    "value": 359,
    "children": []
  },
  {
    "label_en_US": "Bahrain",
    "label_zh_CN": "巴林",
    "abbr": "BH",
    "value": 973,
    "children": []
  },
  {
    "label_en_US": "Burundi",
    "label_zh_CN": "布隆迪",
    "abbr": "BI",
    "value": 257,
    "children": []
  },
  {
    "label_en_US": "Benin",
    "label_zh_CN": "贝宁",
    "abbr": "BJ",
    "value": 229,
    "children": []
  },
  {
    "label_en_US": "Palestine",
    "label_zh_CN": "巴勒斯坦",
    "abbr": "BL",
    "value": 970,
    "children": []
  },
  {
    "label_en_US": "Bermuda Is.",
    "label_zh_CN": "百慕大群岛",
    "abbr": "BM",
    "value": 1441,
    "children": []
  },
  {
    "label_en_US": "Brunei",
    "label_zh_CN": "文莱",
    "abbr": "BN",
    "value": 673,
    "children": []
  },
  {
    "label_en_US": "Bolivia",
    "label_zh_CN": "玻利维亚",
    "abbr": "BO",
    "value": 591,
    "children": []
  },
  {
    "label_en_US": "Brazil",
    "label_zh_CN": "巴西",
    "abbr": "BR",
    "value": 55,
    "children": []
  },
  {
    "label_en_US": "Bahamas",
    "label_zh_CN": "巴哈马",
    "abbr": "BS",
    "value": 1242,
    "children": []
  },
  {
    "label_en_US": "Botswana",
    "label_zh_CN": "博茨瓦纳",
    "abbr": "BW",
    "value": 267,
    "children": []
  },
  {
    "label_en_US": "Belarus",
    "label_zh_CN": "白俄罗斯",
    "abbr": "BY",
    "value": 375,
    "children": []
  },
  {
    "label_en_US": "Belize",
    "label_zh_CN": "伯利兹",
    "abbr": "BZ",
    "value": 501,
    "children": []
  },
  {
    "label_en_US": "Canada",
    "label_zh_CN": "加拿大",
    "abbr": "CA",
    "value": 1,
    "children": []
  },
  {
    "label_en_US": "Is. Cayman",
    "label_zh_CN": "开曼群岛",
    "abbr": "KY",
    "value": 1345,
    "children": []
  },
  {
    "label_en_US": "Central African Republic",
    "label_zh_CN": "中非共和国",
    "abbr": "CF",
    "value": 236,
    "children": []
  },
  {
    "label_en_US": "Congo",
    "label_zh_CN": "刚果",
    "abbr": "CG",
    "value": 242,
    "children": []
  },
  {
    "label_en_US": "Switzerland",
    "label_zh_CN": "瑞士",
    "abbr": "CH",
    "value": 41,
    "children": []
  },
  {
    "label_en_US": "Cook Is.",
    "label_zh_CN": "库克群岛",
    "abbr": "CK",
    "value": 682,
    "children": []
  },
  {
    "label_en_US": "Chile",
    "label_zh_CN": "智利",
    "abbr": "CL",
    "value": 56,
    "children": []
  },
  {
    "label_en_US": "Cameroon",
    "label_zh_CN": "喀麦隆",
    "abbr": "CM",
    "value": 237,
    "children": []
  },
  {
    "label_en_US": "China",
    "label_zh_CN": "中国",
    "abbr": "CN",
    "value": 86,
    "children": []
  },
  {
    "label_en_US": "Colombia",
    "label_zh_CN": "哥伦比亚",
    "abbr": "CO",
    "value": 57,
    "children": []
  },
  {
    "label_en_US": "Costa Rica",
    "label_zh_CN": "哥斯达黎加",
    "abbr": "CR",
    "value": 506,
    "children": []
  },
  {
    "label_en_US": "Czech",
    "label_zh_CN": "捷克",
    "abbr": "CS",
    "value": 420,
    "children": []
  },
  {
    "label_en_US": "Cuba",
    "label_zh_CN": "古巴",
    "abbr": "CU",
    "value": 53,
    "children": []
  },
  {
    "label_en_US": "Cyprus",
    "label_zh_CN": "塞浦路斯",
    "abbr": "CY",
    "value": 357,
    "children": []
  },
  {
    "label_en_US": "Czech Republic",
    "label_zh_CN": "捷克",
    "abbr": "CZ",
    "value": 420,
    "children": []
  },
  {
    "label_en_US": "Germany",
    "label_zh_CN": "德国",
    "abbr": "DE",
    "value": 49,
    "children": []
  },
  {
    "label_en_US": "Djibouti",
    "label_zh_CN": "吉布提",
    "abbr": "DJ",
    "value": 253,
    "children": []
  },
  {
    "label_en_US": "Denmark",
    "label_zh_CN": "丹麦",
    "abbr": "DK",
    "value": 45,
    "children": []
  },
  {
    "label_en_US": "Dominica Rep.",
    "label_zh_CN": "多米尼加共和国",
    "abbr": "DO",
    "value": 1890,
    "children": []
  },
  {
    "label_en_US": "Algeria",
    "label_zh_CN": "阿尔及利亚",
    "abbr": "DZ",
    "value": 213,
    "children": []
  },
  {
    "label_en_US": "Ecuador",
    "label_zh_CN": "厄瓜多尔",
    "abbr": "EC",
    "value": 593,
    "children": []
  },
  {
    "label_en_US": "Estonia",
    "label_zh_CN": "爱沙尼亚",
    "abbr": "EE",
    "value": 372,
    "children": []
  },
  {
    "label_en_US": "Egypt",
    "label_zh_CN": "埃及",
    "abbr": "EG",
    "value": 20,
    "children": []
  },
  {
    "label_en_US": "Spain",
    "label_zh_CN": "西班牙",
    "abbr": "ES",
    "value": 34,
    "children": []
  },
  {
    "label_en_US": "Ethiopia",
    "label_zh_CN": "埃塞俄比亚",
    "abbr": "ET",
    "value": 251,
    "children": []
  },
  {
    "label_en_US": "Finland",
    "label_zh_CN": "芬兰",
    "abbr": "FI",
    "value": 358,
    "children": []
  },
  {
    "label_en_US": "Fiji",
    "label_zh_CN": "斐济",
    "abbr": "FJ",
    "value": 679,
    "children": []
  },
  {
    "label_en_US": "France",
    "label_zh_CN": "法国",
    "abbr": "FR",
    "value": 33,
    "children": []
  },
  {
    "label_en_US": "Gabon",
    "label_zh_CN": "加蓬",
    "abbr": "GA",
    "value": 241,
    "children": []
  },
  {
    "label_en_US": "United Kingdom",
    "label_zh_CN": "英国",
    "abbr": "GB",   //  UK
    "value": 44,
    "children": []
  },
  {
    "label_en_US": "Grenada",
    "label_zh_CN": "格林纳达",
    "abbr": "GD",
    "value": 1809,
    "children": []
  },
  {
    "label_en_US": "Georgia",
    "label_zh_CN": "格鲁吉亚",
    "abbr": "GE",
    "value": 995,
    "children": []
  },
  {
    "label_en_US": "French Guiana",
    "label_zh_CN": "法属圭亚那",
    "abbr": "GF",
    "value": 594,
    "children": []
  },
  {
    "label_en_US": "Ghana",
    "label_zh_CN": "加纳",
    "abbr": "GH",
    "value": 233,
    "children": []
  },
  {
    "label_en_US": "Gibraltar",
    "label_zh_CN": "直布罗陀",
    "abbr": "GI",
    "value": 350,
    "children": []
  },
  {
    "label_en_US": "Gambia",
    "label_zh_CN": "冈比亚",
    "abbr": "GM",
    "value": 220,
    "children": []
  },
  {
    "label_en_US": "Guinea",
    "label_zh_CN": "几内亚",
    "abbr": "GN",
    "value": 224,
    "children": []
  },
  {
    "label_en_US": "Equatorial Guinea",
    "label_zh_CN": "赤道几内亚",
    "abbr": "GQ",
    "value": 240,
    "children": []
  },
  {
    "label_en_US": "Greece",
    "label_zh_CN": "希腊",
    "abbr": "GR",
    "value": 30,
    "children": []
  },
  {
    "label_en_US": "Guatemala",
    "label_zh_CN": "危地马拉",
    "abbr": "GT",
    "value": 502,
    "children": []
  },
  {
    "label_en_US": "Guam",
    "label_zh_CN": "关岛",
    "abbr": "GU",
    "value": 1671,
    "children": []
  },
  {
    "label_en_US": "Guyana",
    "label_zh_CN": "圭亚那",
    "abbr": "GY",
    "value": 592,
    "children": []
  },
  {
    "label_en_US": "Hong Kong (China)",
    "label_zh_CN": "香港（中国）",
    "abbr": "HK",
    "value": 852,
    "children": []
  },
  {
    "label_en_US": "Honduras",
    "label_zh_CN": "洪都拉斯",
    "abbr": "HN",
    "value": 504,
    "children": []
  },
  {
    "label_en_US": "Haiti",
    "label_zh_CN": "海地",
    "abbr": "HT",
    "value": 509,
    "children": []
  },
  {
    "label_en_US": "Hungary",
    "label_zh_CN": "匈牙利",
    "abbr": "HU",
    "value": 36,
    "children": []
  },
  {
    "label_en_US": "Indonesia",
    "label_zh_CN": "印度尼西亚",
    "abbr": "ID",
    "value": 62,
    "children": []
  },
  {
    "label_en_US": "Ireland",
    "label_zh_CN": "爱尔兰",
    "abbr": "IE",
    "value": 353,
    "children": []
  },
  {
    "label_en_US": "Israel",
    "label_zh_CN": "以色列",
    "abbr": "IL",
    "value": 972,
    "children": []
  },
  {
    "label_en_US": "India",
    "label_zh_CN": "印度",
    "abbr": "IN",
    "value": 91,
    "children": []
  },
  {
    "label_en_US": "Iraq",
    "label_zh_CN": "伊拉克",
    "abbr": "IQ",
    "value": 964,
    "children": []
  },
  {
    "label_en_US": "Iran",
    "label_zh_CN": "伊朗",
    "abbr": "IR",
    "value": 98,
    "children": []
  },
  {
    "label_en_US": "Iceland",
    "label_zh_CN": "冰岛",
    "abbr": "IS",
    "value": 354,
    "children": []
  },
  {
    "label_en_US": "Italy",
    "label_zh_CN": "意大利",
    "abbr": "IT",
    "value": 39,
    "children": []
  },
  {
    "label_en_US": "Côte d’Ivoire",
    "label_zh_CN": "科特迪瓦",
    "abbr": "KT",   //  CI
    "value": 225,
    "children": [],
  },
  {
    "label_en_US": "Côte d’Ivoire",
    "label_zh_CN": "科特迪瓦",
    "abbr": "CI",   //  CI
    "value": 225,
    "children": [],
  },
  {
    "label_en_US": "Jamaica",
    "label_zh_CN": "牙买加",
    "abbr": "JM",
    "value": 1876,
    "children": []
  },
  {
    "label_en_US": "Jordan",
    "label_zh_CN": "约旦",
    "abbr": "JO",
    "value": 962,
    "children": []
  },
  {
    "label_en_US": "Japan",
    "label_zh_CN": "日本",
    "abbr": "JP",
    "value": 81,
    "children": []
  },
  {
    "label_en_US": "Kenya",
    "label_zh_CN": "肯尼亚",
    "abbr": "KE",
    "value": 254,
    "children": []
  },
  {
    "label_en_US": "Kyrgyzstan",
    "label_zh_CN": "吉尔吉斯斯坦",
    "abbr": "KG",
    "value": 996,
    "children": []
  },
  {
    "label_en_US": "Cambodia",
    "label_zh_CN": "柬埔寨",
    "abbr": "KH",
    "value": 855,
    "children": []
  },
  {
    "label_en_US": "North Korea",
    "label_zh_CN": "朝鲜",
    "abbr": "KP",
    "value": 850,
    "children": []
  },
  {
    "label_en_US": "South Korea",
    "label_zh_CN": "韩国",
    "abbr": "KR",
    "value": 82,
    "children": []
  },
  {
    "label_en_US": "Kuwait",
    "label_zh_CN": "科威特",
    "abbr": "KW",
    "value": 965,
    "children": []
  },
  {
    "label_en_US": "Kazakhstan",
    "label_zh_CN": "哈萨克斯坦",
    "abbr": "KZ",
    "value": 7,
    "children": []
  },
  {
    "label_en_US": "Laos",
    "label_zh_CN": "老挝",
    "abbr": "LA",
    "value": 856,
    "children": []
  },
  {
    "label_en_US": "Lebanon",
    "label_zh_CN": "黎巴嫩",
    "abbr": "LB",
    "value": 961,
    "children": []
  },
  {
    "label_en_US": "St.Lucia",
    "label_zh_CN": "圣卢西亚",
    "abbr": "LC",
    "value": 1758,
    "children": []
  },
  {
    "label_en_US": "Liechtenstein",
    "label_zh_CN": "列支敦士登",
    "abbr": "LI",
    "value": 423,
    "children": []
  },
  {
    "label_en_US": "Sri Lanka",
    "label_zh_CN": "斯里兰卡",
    "abbr": "LK",
    "value": 94,
    "children": []
  },
  {
    "label_en_US": "Liberia",
    "label_zh_CN": "利比里亚",
    "abbr": "LR",
    "value": 231,
    "children": []
  },
  {
    "label_en_US": "Lesotho",
    "label_zh_CN": "莱索托",
    "abbr": "LS",
    "value": 266,
    "children": []
  },
  {
    "label_en_US": "Lithuania",
    "label_zh_CN": "立陶宛",
    "abbr": "LT",
    "value": 370,
    "children": []
  },
  {
    "label_en_US": "Luxembourg",
    "label_zh_CN": "卢森堡",
    "abbr": "LU",
    "value": 352,
    "children": []
  },
  {
    "label_en_US": "Latvia",
    "label_zh_CN": "拉脱维亚",
    "abbr": "LV",
    "value": 371,
    "children": []
  },
  {
    "label_en_US": "Libya",
    "label_zh_CN": "利比亚",
    "abbr": "LY",
    "value": 218,
    "children": []
  },
  {
    "label_en_US": "Morocco",
    "label_zh_CN": "摩洛哥",
    "abbr": "MA",
    "value": 212,
    "children": []
  },
  {
    "label_en_US": "Monaco",
    "label_zh_CN": "摩纳哥",
    "abbr": "MC",
    "value": 377,
    "children": []
  },
  {
    "label_en_US": "Moldova, Republic of",
    "label_zh_CN": "摩尔多瓦",
    "abbr": "MD",
    "value": 373,
    "children": []
  },
  {
    "label_en_US": "Madagascar",
    "label_zh_CN": "马达加斯加",
    "abbr": "MG",
    "value": 261,
    "children": []
  },
  {
    "label_en_US": "Mali",
    "label_zh_CN": "马里",
    "abbr": "ML",
    "value": 223,
    "children": []
  },
  {
    "label_en_US": "Myanmar",
    "label_zh_CN": "缅甸",
    "abbr": "MM",
    "value": 95,
    "children": []
  },
  {
    "label_en_US": "Mongolia",
    "label_zh_CN": "蒙古",
    "abbr": "MN",
    "value": 976,
    "children": []
  },
  {
    "label_en_US": "Macau (China)",
    "label_zh_CN": "澳门（中国）",
    "abbr": "MO",
    "value": 853,
    "children": []
  },
  {
    "label_en_US": "Montserrat Is",
    "label_zh_CN": "蒙特塞拉特岛",
    "abbr": "MS",
    "value": 1664,
    "children": []
  },
  {
    "label_en_US": "Malta",
    "label_zh_CN": "马耳他",
    "abbr": "MT",
    "value": 356,
    "children": []
  },
  {
    "label_en_US": "Is",
    "label_zh_CN": "Mariana",
    "abbr": "马里亚那群岛",
    "value": 1670,
    "children": []
  },
  // 马提尼克 	Martinique 	596
  {
    "label_en_US": "Mauritius",
    "label_zh_CN": "毛里求斯",
    "abbr": "MU",
    "value": 230,
    "children": []
  },
  {
    "label_en_US": "Maldives",
    "label_zh_CN": "马尔代夫",
    "abbr": "MV",
    "value": 960,
    "children": []
  },
  {
    "label_en_US": "Malawi",
    "label_zh_CN": "马拉维",
    "abbr": "MW",
    "value": 265,
    "children": []
  },
  {
    "label_en_US": "Mexico",
    "label_zh_CN": "墨西哥",
    "abbr": "MX",
    "value": 52,
    "children": []
  },
  {
    "label_en_US": "Malaysia",
    "label_zh_CN": "马来西亚",
    "abbr": "MY",
    "value": 60,
    "children": []
  },
  {
    "label_en_US": "Mozambique",
    "label_zh_CN": "莫桑比克",
    "abbr": "MZ",
    "value": 258,
    "children": []
  },
  {
    "label_en_US": "Namibia",
    "label_zh_CN": "纳米比亚",
    "abbr": "NA",
    "value": 264,
    "children": []
  },
  {
    "label_en_US": "Nigeria",
    "label_zh_CN": "尼日利亚",
    "abbr": "NG",
    "value": 234,
    "children": []
  },
  {
    "label_en_US": "Nicaragua",
    "label_zh_CN": "尼加拉瓜",
    "abbr": "NI",
    "value": 505,
    "children": []
  },
  {
    "label_en_US": "Netherlands",
    "label_zh_CN": "荷兰",
    "abbr": "NL",
    "value": 31,
    "children": []
  },
  {
    "label_en_US": "Norway",
    "label_zh_CN": "挪威",
    "abbr": "NO",
    "value": 47,
    "children": []
  },
  {
    "label_en_US": "Nepal",
    "label_zh_CN": "尼泊尔",
    "abbr": "NP",
    "value": 977,
    "children": []
  },
  {
    "label_en_US": "Niger",
    "label_zh_CN": "尼日尔",
    "abbr": "NE",
    "value": 227,
    "children": []
  },
  {
    "label_en_US": "Netherlands Antillse",
    "label_zh_CN": "荷属安的列斯",
    "abbr": "AN",
    "value": 599,
    "children": []
  },
  {
    "label_en_US": "Nauru",
    "label_zh_CN": "瑙鲁",
    "abbr": "NR",
    "value": 674,
    "children": []
  },
  {
    "label_en_US": "New Zealand",
    "label_zh_CN": "新西兰",
    "abbr": "NZ",
    "value": 64,
    "children": []
  },
  {
    "label_en_US": "Oman",
    "label_zh_CN": "阿曼",
    "abbr": "OM",
    "value": 968,
    "children": []
  },
  {
    "label_en_US": "Panama",
    "label_zh_CN": "巴拿马",
    "abbr": "PA",
    "value": 507,
    "children": []
  },
  {
    "label_en_US": "Peru",
    "label_zh_CN": "秘鲁",
    "abbr": "PE",
    "value": 51,
    "children": []
  },
  {
    "label_en_US": "French Polynesia",
    "label_zh_CN": "法属玻利尼西亚",
    "abbr": "PF",
    "value": 689,
    "children": []
  },
  {
    "label_en_US": "Papua New Guinea",
    "label_zh_CN": "巴布亚新几内亚",
    "abbr": "PG",
    "value": 675,
    "children": []
  },
  {
    "label_en_US": "Philippines",
    "label_zh_CN": "菲律宾",
    "abbr": "PH",
    "value": 63,
    "children": []
  },
  {
    "label_en_US": "Pakistan",
    "label_zh_CN": "巴基斯坦",
    "abbr": "PK",
    "value": 92,
    "children": []
  },
  {
    "label_en_US": "Poland",
    "label_zh_CN": "波兰",
    "abbr": "PL",
    "value": 48,
    "children": []
  },
  {
    "label_en_US": "Puerto Rico",
    "label_zh_CN": "波多黎各",
    "abbr": "PR",
    "value": 1787,
    "children": []
  },
  {
    "label_en_US": "Portugal",
    "label_zh_CN": "葡萄牙",
    "abbr": "PT",
    "value": 351,
    "children": []
  },
  {
    "label_en_US": "Paraguay",
    "label_zh_CN": "巴拉圭",
    "abbr": "PY",
    "value": 595,
    "children": []
  },
  {
    "label_en_US": "Qatar",
    "label_zh_CN": "卡塔尔",
    "abbr": "QA",
    "value": 974,
    "children": []
  },
  // 留尼旺 	Reunion 	262
  {
    "label_en_US": "Romania",
    "label_zh_CN": "罗马尼亚",
    "abbr": "RO",
    "value": 40,
    "children": []
  },
  {
    "label_en_US": "Russia",
    "label_zh_CN": "俄罗斯",
    "abbr": "RU",
    "value": 7,
    "children": []
  },
  {
    "label_en_US": "Saudi Arabia",
    "label_zh_CN": "沙特阿拉伯",
    "abbr": "SA",
    "value": 966,
    "children": []
  },
  {
    "label_en_US": "Solomon Is",
    "label_zh_CN": "所罗门群岛",
    "abbr": "SB",
    "value": 677,
    "children": []
  },
  {
    "label_en_US": "Seychelles",
    "label_zh_CN": "塞舌尔",
    "abbr": "SC",
    "value": 248,
    "children": []
  },
  {
    "label_en_US": "Sudan",
    "label_zh_CN": "苏丹",
    "abbr": "SD",
    "value": 249,
    "children": []
  },
  {
    "label_en_US": "South Sudan",
    "label_zh_CN": "南苏丹",
    "abbr": "SS",
    "value": 211,
    "children": []
  },
  {
    "label_en_US": "Sweden",
    "label_zh_CN": "瑞典",
    "abbr": "SE",
    "value": 46,
    "children": []
  },
  {
    "label_en_US": "Singapore",
    "label_zh_CN": "新加坡",
    "abbr": "SG",
    "value": 65,
    "children": []
  },
  {
    "label_en_US": "Slovenia",
    "label_zh_CN": "斯洛文尼亚",
    "abbr": "SI",
    "value": 386,
    "children": []
  },
  {
    "label_en_US": "Slovakia",
    "label_zh_CN": "斯洛伐克",
    "abbr": "SK",
    "value": 421,
    "children": []
  },
  {
    "label_en_US": "Sierra Leone",
    "label_zh_CN": "塞拉利昂",
    "abbr": "SL",
    "value": 232,
    "children": []
  },
  {
    "label_en_US": "San Marino",
    "label_zh_CN": "圣马力诺",
    "abbr": "SM",
    "value": 378,
    "children": []
  },
  // {
  //   "label_en_US": "Eastern",
  //   "label_zh_CN": "Samoa",
  //   "abbr": "东萨摩亚(美)",
  //   "value": 684,
  //   "children": []
  // },
  // {
  //   "label_en_US": "Marino",
  //   "label_zh_CN": "San",
  //   "abbr": "西萨摩亚",
  //   "value": 685,
  //   "children": []
  // },
  {
    "label_en_US": "Senegal",
    "label_zh_CN": "塞内加尔",
    "abbr": "SN",
    "value": 221,
    "children": []
  },
  {
    "label_en_US": "Somalia",
    "label_zh_CN": "索马里",
    "abbr": "SO",
    "value": 252,
    "children": []
  },
  {
    "label_en_US": "Suriname",
    "label_zh_CN": "苏里南",
    "abbr": "SR",
    "value": 597,
    "children": []
  },
  {
    "label_en_US": "Sao Tome and Principe",
    "label_zh_CN": "圣多美和普林西比",
    "abbr": "ST",
    "value": 239,
    "children": []
  },
  {
    "label_en_US": "EI Salvador",
    "label_zh_CN": "萨尔瓦多",
    "abbr": "SV",
    "value": 503,
    "children": []
  },
  {
    "label_en_US": "Syria",
    "label_zh_CN": "叙利亚",
    "abbr": "SY",
    "value": 963,
    "children": []
  },
  {
    "label_en_US": "Swaziland",
    "label_zh_CN": "斯威士兰",
    "abbr": "SZ",
    "value": 268,
    "children": []
  },
  {
    "label_en_US": "Chad",
    "label_zh_CN": "乍得",
    "abbr": "TD",
    "value": 235,
    "children": []
  },
  {
    "label_en_US": "Togolaise",
    "label_zh_CN": "多哥",
    "abbr": "TG",
    "value": 228,
    "children": []
  },
  {
    "label_en_US": "Thailand",
    "label_zh_CN": "泰国",
    "abbr": "TH",
    "value": 66,
    "children": []
  },
  {
    "label_en_US": "Timor-Leste",
    "label_zh_CN": "东帝汶",
    "abbr": "TL",
    "value": 670,
    "children": [],
  },
  {
    "label_en_US": "Tajikstan",
    "label_zh_CN": "塔吉克斯坦",
    "abbr": "TJ",
    "value": 992,
    "children": []
  },
  {
    "label_en_US": "Turkmenistan",
    "label_zh_CN": "土库曼斯坦",
    "abbr": "TM",
    "value": 993,
    "children": []
  },
  {
    "label_en_US": "Tunisia",
    "label_zh_CN": "突尼斯",
    "abbr": "TN",
    "value": 216,
    "children": []
  },
  {
    "label_en_US": "Tonga",
    "label_zh_CN": "汤加",
    "abbr": "TO",
    "value": 676,
    "children": []
  },
  {
    "label_en_US": "Turkey",
    "label_zh_CN": "土耳其",
    "abbr": "TR",
    "value": 90,
    "children": []
  },
  {
    "label_en_US": "Trinidad and Tobago",
    "label_zh_CN": "特立尼达和多巴哥",
    "abbr": "TT",
    "value": 1809,
    "children": []
  },
  {
    "label_en_US": "Taiwan (China)",
    "label_zh_CN": "台湾（中国）",
    "abbr": "TW",
    "value": 886,
    "children": []
  },
  {
    "label_en_US": "Tanzania",
    "label_zh_CN": "坦桑尼亚",
    "abbr": "TZ",
    "value": 255,
    "children": []
  },
  {
    "label_en_US": "Ukraine",
    "label_zh_CN": "乌克兰",
    "abbr": "UA",
    "value": 380,
    "children": []
  },
  {
    "label_en_US": "Uganda",
    "label_zh_CN": "乌干达",
    "abbr": "UG",
    "value": 256,
    "children": []
  },
  {
    "label_en_US": "United States",
    "label_zh_CN": "美国",
    "abbr": "US",
    "value": 1,
    "children": []
  },
  {
    "label_en_US": "Uruguay",
    "label_zh_CN": "乌拉圭",
    "abbr": "UY",
    "value": 598,
    "children": []
  },
  {
    "label_en_US": "Uzbekistan",
    "label_zh_CN": "乌兹别克斯坦",
    "abbr": "UZ",
    "value": 998,
    "children": []
  },
  {
    "label_en_US": "Saint Vincent",
    "label_zh_CN": "圣文森特岛",
    "abbr": "VC",
    "value": 1784,
    "children": []
  },
  {
    "label_en_US": "Venezuela",
    "label_zh_CN": "委内瑞拉",
    "abbr": "VE",
    "value": 58,
    "children": []
  },
  {
    "label_en_US": "Vietnam",
    "label_zh_CN": "越南",
    "abbr": "VN",
    "value": 84,
    "children": []
  },
  {
    "label_en_US": "Yemen",
    "label_zh_CN": "也门",
    "abbr": "YE",
    "value": 967,
    "children": []
  },
  {
    "label_en_US": "Yugoslavia",
    "label_zh_CN": "南斯拉夫",
    "abbr": "YU",
    "value": 381,
    "children": []
  },
  {
    "label_en_US": "South Africa",
    "label_zh_CN": "南非",
    "abbr": "ZA",
    "value": 27,
    "children": []
  },
  {
    "label_en_US": "Zambia",
    "label_zh_CN": "赞比亚",
    "abbr": "ZM",
    "value": 260,
    "children": []
  },
  {
    "label_en_US": "Zaire",
    "label_zh_CN": "扎伊尔",
    "abbr": "ZR",
    "value": 243,
    "children": []
  },
  {
    "label_en_US": "Democratic Republic of the Congo",
    "label_zh_CN": "刚果（金）",
    "abbr": "CD",
    "value": 243,
    "children": []
  },
  {
    "label_en_US": "Zimbabwe",
    "label_zh_CN": "津巴布韦",
    "abbr": "ZW",
    "value": 263,
    "children": []
  },
  {
    "label_en_US": "Republic of Rwanda",
    "label_zh_CN": "卢旺达",
    "abbr": "RW",
    "value": 250,
    "children": []
  },
  {
    "label_en_US": "The Islamic Republic of Mauritania",
    "label_zh_CN": "毛里塔尼亚",
    "abbr": "MR",
    "value": 222,
    "children": []
  },
  {
    "label_en_US": "Tokelau",
    "label_zh_CN": "托克劳",
    "abbr": "TK",
    "value": 64,
    "children": []
  },
  {
    "label_en_US": "The Republic of Guinea-Bissau",
    "label_zh_CN": "几内亚比绍",
    "abbr": "GW",
    "value": 245,
    "children": []
  },
  {
    "label_en_US": "Bosnia and Herzegovina",
    "label_zh_CN": "波斯尼亚和黑塞哥维那",
    "abbr": "BA",
    "value": 387,
    "children": []
  },
  {
    "label_en_US": "The Republic of Croatia",
    "label_zh_CN": "克罗地亚共和国",
    "abbr": "HR",
    "value": 385,
    "children": []
  },
  {
    "label_en_US": "Union of Comoros",
    "label_zh_CN": "科摩罗联盟",
    "abbr": "KM",
    "value": 269,
    "children": []
  },
  {
    "label_en_US": "The Republic of Serbia",
    "label_zh_CN": "塞尔维亚共和国",
    "abbr": "RS",
    "value": 381,
    "children": []
  },
  {
    "label_en_US": "The Democratic Republic of Sao Tome and Principe",
    "label_zh_CN": "圣多美和普林西比",
    "abbr": "TP",
    "value": 239,
    "children": []
  },
];

export default area
