import React from 'react';
import { FaSpinner } from 'react-icons/fa';

import LoadingCss from '../../css/modules/LoadingSpinner.module.css';

function LoadingSpinner() {

  return (
    <div className={LoadingCss.loading}>
        <div className={LoadingCss.spinner}>
          <FaSpinner/>
          <p>Loading...</p>
        </div>
    </div>
  );
}

export default LoadingSpinner;