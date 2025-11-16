import React from 'react';
import { ImageViewer, Image, Space } from 'tdesign-react';
import { BrowseIcon } from 'tdesign-icons-react';

// const img = 'https://tdesign.gtimg.com/demo/demo-image-1.png';
export default function BasicImageViewer(img, width=50, height=50, alt='pic') {
  const trigger = ({ open }) => {
    const mask = (
      <div
        style={{
          background: 'rgba(0,0,0,.6)',
          color: '#fff',
          height: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
        onClick={open}
      >
        <span>
          <BrowseIcon size="16px" name={'browse'} />
        </span>
      </div>
    );
    return (
      <Image
        alt={alt}
        src={img}
        overlayContent={mask}
        overlayTrigger="hover"
        fit="contain"
        style={{
          width: width,
          height: height,
          border: '4px solid var(--td-bg-color-secondarycontainer)',
          borderRadius: 'var(--td-radius-medium)',
          backgroundColor: '#fff',
        }}
      />
    );
  };
  return (
    <Space breakLine size={16}>
      <ImageViewer trigger={trigger} images={[img]} />

      {/* TODO: fix visible=true can not show image previewer */}
      {/* <ImageViewer images={[img]} visible={true} /> */}
    </Space>
  );
}