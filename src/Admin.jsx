import React from "react";
// user state control
import { AdminProvider } from '../context/AdminContext';

// custom hook
import { UseAdmin } from '../hook/UseAdmin';

import AdminDashboard from '../component/admin/AdminDashboard';
import AdminLogin from '../component/admin/AdminLogin';

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
    <>
        <AdminProvider>
            <AuthTof />
        </AdminProvider>
    </>  
    );
}

export default Admin;