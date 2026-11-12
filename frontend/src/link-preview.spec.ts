import indexHtml from './index.html' with { loader: 'text' };
import image from '../public/og-image.jpg' with { loader: 'binary' };

const SITE = 'https://cookie-clicker-he2s.onrender.com';

function jpegSize(bytes: Uint8Array) {
  let offset = 2;
  while (offset < bytes.length) {
    const marker = bytes[offset + 1];
    const isFrameHeader = marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker);
    if (isFrameHeader) {
      return {
        height: (bytes[offset + 5] << 8) | bytes[offset + 6],
        width: (bytes[offset + 7] << 8) | bytes[offset + 8],
      };
    }
    offset += 2 + ((bytes[offset + 2] << 8) | bytes[offset + 3]);
  }
  throw new Error('not a JPEG');
}

describe('link preview', () => {
  const head = new DOMParser().parseFromString(indexHtml, 'text/html').head;
  const meta = (key: string) =>
    head.querySelector(`meta[property="${key}"], meta[name="${key}"]`)?.getAttribute('content');

  it('describes the page for Open Graph and X', () => {
    expect(meta('og:type')).toBe('website');
    expect(meta('og:site_name')).toBe('Cookie Clicker');
    expect(meta('og:title')).toBe('Cookie Clicker');
    expect(meta('og:description')).toBeTruthy();
    expect(meta('og:image')).toBe(`${SITE}/og-image.jpg`);
    expect(meta('og:image:alt')).toBeTruthy();
    expect(meta('twitter:card')).toBe('summary_large_image');
  });

  it('gives the real size of the preview image', () => {
    const { width, height } = jpegSize(image);

    expect({ width, height }).toEqual({ width: 1200, height: 630 });
    expect(meta('og:image:width')).toBe(String(width));
    expect(meta('og:image:height')).toBe(String(height));
    expect(image.length).toBeLessThan(300 * 1024);
  });
});
