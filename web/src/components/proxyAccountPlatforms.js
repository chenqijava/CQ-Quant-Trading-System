/**
 *  src中引用了该文件,修改后最好build一下
 */
const fieldOpt = {
  rules: [
    {
      required: true,
      message: '不能为空'
    },
  ]
};

const getParamOpt = (key, {label = key, required = false, placeholder = label} = {}) => ({
      key,
      form: {
        label,
      },
      item: {
        placeholder,
      },
      fieldOpt: required ? fieldOpt : undefined,
    }
);

const platforms = [
  {
    label: '聚合平台',
    value: 'aggregationPlatform',
  },
  {
    label: 'he',
    value: 'he',
    params: [
      getParamOpt('token', {required: true}),
      getParamOpt('id'),
    ]
  },
  {
    label: 'leo',
    value: 'leo'
  },
  {
    label: 'ss5api',
    value: 'ss5api'
  },
  {
    label: 'rola',
    value: 'rola'
  },
  {
    label: 'doveip',
    value: 'doveip'
  },
  {
    label: 'coralip',
    value: 'coralip'
  },
  {
    label: 'iproyal',
    value: 'iproyal'
  },
  {
    label: 'skyip',
    value: 'skyip',
    //  自定义平台参数
    params: [
      getParamOpt('token', {required: true}),
      getParamOpt('id', {label: '代理类型', placeholder: "4g/datacenter/不填"}),
    ],
  },
  {
    label: 'cakeip',
    value: 'cakeip',
  }
];


module.exports = platforms;
