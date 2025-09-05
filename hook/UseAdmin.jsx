import { useContext } from 'react';
import { AdminContext } from '../context/AdminContext';

// useAdmin 커스텀 훅
export function UseAdmin() {
  const AdContext = useContext(AdminContext);

  if(!AdContext){
    return;
  }
  
  return AdContext;
}