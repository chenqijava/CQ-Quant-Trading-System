import area from './area'

let areaCode = Object.assign(area);

for (let c in areaCode){
  delete areaCode[c].children
}

export default areaCode
