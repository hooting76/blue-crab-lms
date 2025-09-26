import "../../../css/Communities/Notice-ui.css";

export default function Pagination({ page, size, total, onChange }) {
    const pages = Math.max(1, Math.ceil(total / size)); // 총 페이지 수

    const handleClick = (newPage) => {
        if (typeof onChange === "function" && newPage !== page) {
            onChange(newPage);
        }
    };

    return (
        <nav className="pagination">
            <a onClick={() => handleClick(Math.max(1, page - 1))}>previous</a>
            {Array.from({ length: pages }, (_, i) => i + 1).map(p => (
                <a
                    key={p}
                    onClick={() => handleClick(p)}
                    className={p === page ? "active" : ""}
                >
                    {p}
                </a>
            ))}
            <a onClick={() => handleClick(Math.min(pages, page + 1))}>next</a>
        </nav>
    );
}
