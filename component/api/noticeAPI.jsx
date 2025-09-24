async function getNotices() {
  const response = await fetch('https://bluecrab.chickenkiller.com/Bluecrab-1.0.0/api/boards/list', {
    headers : {
      'Authorization' : `Bearer ${acessToken}`,
      'Content-Type' : 'application/json'
    }
  }); 
  if (!response.ok) {
    throw new Error('Failed to fetch notices');
  }
  const items = await response.json();
  return items;
};

export default getNotices;