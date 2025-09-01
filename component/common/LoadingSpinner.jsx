import React from 'react';

function LoadingSpinner({ size = 'large', message = '로딩 중...' }) {
  const sizeClasses = {
    small: 'h-6 w-6',
    medium: 'h-8 w-8', 
    large: 'h-12 w-12'
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="text-center">
        <div className={`animate-spin rounded-full ${sizeClasses[size]} border-b-2 border-blue-500 mx-auto mb-4`}>
        </div>
        <p className="text-gray-600">{message}</p>
        
        {/* 추가적인 로딩 점들 */}
        <div className="flex justify-center mt-2 space-x-1">
          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce"></div>
          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
        </div>
      </div>
    </div>
  );
}

// 인라인 스피너 버전 (페이지 전체가 아닌 작은 영역용)
export function InlineSpinner({ size = 'small' }) {
  const sizeClasses = {
    small: 'h-4 w-4',
    medium: 'h-6 w-6'
  };

  return (
    <div className={`animate-spin rounded-full ${sizeClasses[size]} border-b-2 border-blue-500`}>
    </div>
  );
}

export default LoadingSpinner;