import React from "react";
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';

// user state control
import { AdminProvider } from '../context/AdminContext';

// custom hook
import { UseAdmin } from '../hook/UseAdmin';


import InAppFunction from '../component/common/InAppFunc';
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
    <BrowserRouter>
        <div id="wrap">
            {isAuthenticated ? null : null}
            <div id="content">
                {isAuthenticated 
                ? <AdminDashboard /> 
                : <AdminLogin/>}
            </div>
            {isAuthenticated ? null : null}
        </div>
    </BrowserRouter>
    );
}

function Admin() {
    return(
        <>  
            <InAppFilter />
            <AdminProvider>
                <AuthTof />
            </AdminProvider>
        </>  
    );
}

export default Admin;