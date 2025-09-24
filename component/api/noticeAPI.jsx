export async function getNotices(BOARD_CODE, page, size) {
    const queryParams = new URLSearchParams({
        BOARD_CODE,
        page,
        size
    });

    const response = await fetch(`https://bluecrab.chickenkiller.com/Bluecrab-1.0.0/api/boards/list?${queryParams.toString()}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
}
