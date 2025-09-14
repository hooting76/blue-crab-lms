import React from "react";
import { UseAdmin } from '../../hook/UseAdmin';

import AdminDashCss from '../../css/admin/module/AdminDashboard.module.css';

export default function AdminDashboard() {

    return (<>
        <main className={AdminDashCss.main}>
            <h1>Admin Dashboard</h1>
        </main>
    </>);
};