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
  const data = await response.json();
  return data;
};

export default getNotices;