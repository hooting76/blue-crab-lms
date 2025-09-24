export async function getNotices(BOARD_CODE, page, size) {
    const response = await fetch("https://bluecrab.chickenkiller.com/Bluecrab-1.0.0/api/boards/list", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            BOARD_CODE,
            page,
            size
        })
    });

    if (!response.ok) {
        // HTTP 에러 처리
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
}