import React from 'react';
import { Pagination } from 'tdesign-react';

const CustomPagination = (props) => {
  return (
    <Pagination
      {...props}
      pageSizeOptions={[10, 20, 30, 40, 50, 100, 200, 300, 400, 500]}
    />
  );
};

export default CustomPagination;