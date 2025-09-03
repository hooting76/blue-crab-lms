import React from 'react';
import FooterCss from '../../css/modules/Footer.module.css';

export default function Footer() {
  return (
    <footer className="footer">
        <div className={FooterCss.footer_text}>
            <p>© 2025 BlueCrab Academy Academic System</p>
            <p className={FooterCss.subLink}>
                <a href="" target='_blank'>이용약관</a>
                <a href="" target='_blank'>개인정보처리방침</a>
                <a href="" target='_blank'>개발자</a>
                <a href="" target='_blank'>GitHub링크</a>
            </p>
            <p>
                <span>문의: help@bluecrab.ac.kr</span>
                <span><a href="tel: 02-123-4567">02-123-4567</a></span>
            </p>
        </div>
        <div className={FooterCss.footer_logo}>
            <h6>
                <picture className={FooterCss.logoImg}>
                    <img src='./favicon/android-icon-72x72.png' alt='Logo'/>
                </picture>
                Blue-Crab LMS
            </h6>            
        </div>
    </footer>
  )};