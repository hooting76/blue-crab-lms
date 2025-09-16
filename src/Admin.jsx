import React from "react";
import { BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';

// user state control
import { AdminProvider } from '../context/AdminContext';

// custom hook
import { UseAdmin } from '../hook/UseAdmin';

import AdminBdCss from "../css/admin/module/admin.module.css";

import InAppFunction from '../component/common/InAppFunc';
import LoadingSpinner from '../component/common/LoadingSpinner';
import AdminDashboard from '../component/admin/AdminDashboard';
import AdminLogin from '../component/admin/AdminLogin';

import AdHeader from "../component/admin/common/AdHeader";
import AdFooter from "../component/admin/common/AdFooter";
import AdNav from "../component/admin/common/AdNav";

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
        <div id="wrap" className={AdminBdCss.wrap}>
            {isAuthenticated ? <AdNav/> : null}
            <div id="content" className={AdminBdCss.content}>
                {isAuthenticated ? <AdHeader /> : null }
                {isAuthenticated ? <AdminDashboard /> : <AdminLogin/>}
                {isAuthenticated ? <AdFooter /> : null }
            </div>
        </div>
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