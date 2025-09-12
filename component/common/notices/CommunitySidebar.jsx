// 우측 커뮤니티 메뉴
import { Link } from "react-router-dom";

export default function CommunitySidebar() {
    return(
        <nav className="community-sidebar">
            <h3>커뮤니티</h3>
            <ul>
                <li><Link to="/community/academy">학사공지</Link></li>
                <li><Link to="/community/admin">행정공지</Link></li>
                <li><Link to="/community/etc">기타공지</Link></li>
            </ul>
        </nav>
    );
}