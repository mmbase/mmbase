function setIframeHeight() {
    const iframes = document.querySelectorAll('div.mm_c.iframe iframe');
    iframes.forEach(iframe => {
        iframe.style.height = window.innerHeight + 'px';
    });
}

document.addEventListener('DOMContentLoaded', setIframeHeight);
window.addEventListener('resize', setIframeHeight);
