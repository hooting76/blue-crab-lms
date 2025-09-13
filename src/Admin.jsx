import React from "react";

// user state control
import { AdminProvider } from '../context/AdminContext';
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';
import InAppFunction from '../component/common/InAppFunc';

// custom hook
import { UseAdmin } from '../hook/UseAdmin';

import LoadingSpinner from '../component/common/LoadingSpinner';
import AdminDashboard from '../component/admin/AdminDashboard';
import AdminLogin from '../component/admin/AdminLogin';

// InApp filter function
function InAppFilter(){
  const userAgent = navigator.userAgent.toLowerCase();
  const targetUrl = location.href;

  return(
    InAppFunction(userAgent, targetUrl)
  );
};

function AuthTof(){
    const { isAuthenticated, isLoading } = UseAdmin();    

    if (isLoading) {
        return <LoadingSpinner/>
    }

    return (
        <div id="wrap">
            <div id="content">
                {isAuthenticated 
                ? <AdminDashboard /> 
                : <AdminLogin/>}
            </div>
        </div>
    );
}

function Admin() {
    return(
    <>  <InAppFilter />
        <AdminProvider>
            <AuthTof />
        </AdminProvider>
    </>  
    );
}

export default Admin;