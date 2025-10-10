import React, { useState } from 'react';
import { Calendar, Clock, Users, AlertCircle, CheckCircle, XCircle, Filter, Search } from 'lucide-react';

const AdminBookingSystem = () => {
  const [activeTab, setActiveTab] = useState('pending'); // 'pending', 'all', 'stats'
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [approvalNote, setApprovalNote] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterFacility, setFilterFacility] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const pendingBookings = [
    {
      id: 2,
      facility: '스터디룸 201호',
      date: '2025-10-08',
      time: '10:00-12:00',
      status: 'pending',
      purpose: '중간고사 스터디',
      participants: 4,
      equipment: '화이트보드',
      userName: '김철수',
      userCode: '20241234',
      userEmail: 'chulsoo@university.ac.kr',
      requestedAt: '2025-10-01 09:30'
    },
    {
      id: 5,
      facility: '세미나실 302호',
      date: '2025-10-09',
      time: '14:00-16:00',
      status: 'pending',
      purpose: '동아리 정기 회의',
      participants: 12,
      equipment: '빔프로젝터, 화이트보드',
      userName: '이영희',
      userCode: '20242345',
      userEmail: 'younghee@university.ac.kr',
      requestedAt: '2025-10-01 11:20'
    },
    {
      id: 6,
      facility: '대강당',
      date: '2025-10-15',
      time: '18:00-20:00',
      status: 'pending',
      purpose: '학과 축제 준비 회의',
      participants: 30,
      equipment: '무대조명, 음향시설, 빔프로젝터',
      userName: '박민수',
      userCode: '20243456',
      userEmail: 'minsu@university.ac.kr',
      requestedAt: '2025-10-01 14:15'
    }
  ];

  const allBookings = [
    {
      id: 1,
      facility: '세미나실 301호',
      date: '2025-10-05',
      time: '14:00-16:00',
      status: 'approved',
      purpose: '컴퓨터공학과 캡스톤 팀 프로젝트 회의',
      participants: 8,
      equipment: '빔프로젝터, 음향시설',
      userName: '홍길동',
      userCode: '20240123',
      userEmail: 'gildong@university.ac.kr',
      requestedAt: '2025-09-28 10:00',
      processedAt: '2025-09-28 10:30',
      adminNote: '행사 진행에 필요한 장비 세팅 완료하였습니다.'
    },
    ...pendingBookings,
    {
      id: 4,
      facility: '체육관',
      date: '2025-10-12',
      time: '18:00-20:00',
      status: 'rejected',
      purpose: '농구 동아리 연습',
      participants: 15,
      equipment: '농구골대, 음향시설',
      userName: '정수진',
      userCode: '20244567',
      userEmail: 'sujin@university.ac.kr',
      requestedAt: '2025-10-01 16:00',
      processedAt: '2025-10-01 16:30',
      rejectionReason: '해당 시간대는 정기 행사로 사용 불가합니다.'
    }
  ];

  const stats = {
    today: {
      total: 12,
      inUse: 3,
      upcoming: 9
    },
    pending: pendingBookings.length,
    thisWeek: 45,
    thisMonth: 180
  };

  const facilities = [
    { value: '', label: '전체 시설' },
    { value: '스터디룸', label: '스터디룸' },
    { value: '세미나실', label: '세미나실' },
    { value: '대강당', label: '대강당' },
    { value: '체육관', label: '체육관' }
  ];

  const statuses = [
    { value: '', label: '전체 상태' },
    { value: 'pending', label: '승인 대기' },
    { value: 'approved', label: '승인 완료' },
    { value: 'rejected', label: '반려됨' }
  ];

  const handleApprove = (bookingId) => {
    alert(`예약 ID ${bookingId} 승인 완료\n비고: ${approvalNote || '없음'}`);
    setSelectedBooking(null);
    setApprovalNote('');
  };

  const handleReject = (bookingId) => {
    if (!rejectionReason.trim()) {
      alert('반려 사유를 입력해주세요.');
      return;
    }
    alert(`예약 ID ${bookingId} 반려 완료\n사유: ${rejectionReason}`);
    setSelectedBooking(null);
    setRejectionReason('');
  };

  const filteredBookings = allBookings.filter(booking => {
    const matchStatus = !filterStatus || booking.status === filterStatus;
    const matchFacility = !filterFacility || booking.facility.includes(filterFacility);
    const matchSearch = !searchTerm || 
      booking.userName.includes(searchTerm) || 
      booking.userCode.includes(searchTerm) ||
      booking.facility.includes(searchTerm);
    return matchStatus && matchFacility && matchSearch;
  });

  const getStatusBadge = (status) => {
    switch(status) {
      case 'approved':
        return <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">승인 완료</span>;
      case 'pending':
        return <span className="px-3 py-1 bg-yellow-100 text-yellow-700 rounded-full text-sm font-medium">승인 대기</span>;
      case 'rejected':
        return <span className="px-3 py-1 bg-red-100 text-red-700 rounded-full text-sm font-medium">반려됨</span>;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-2">시설물 예약 관리</h1>
          <p className="text-gray-600">예약 승인 및 전체 현황을 관리합니다</p>
        </div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="text-sm text-gray-600 mb-1">승인 대기</div>
            <div className="text-3xl font-bold text-yellow-600">{stats.pending}</div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="text-sm text-gray-600 mb-1">오늘 예약</div>
            <div className="text-3xl font-bold text-blue-600">{stats.today.total}</div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="text-sm text-gray-600 mb-1">이번 주</div>
            <div className="text-3xl font-bold text-gray-700">{stats.thisWeek}</div>
          </div>
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="text-sm text-gray-600 mb-1">이번 달</div>
            <div className="text-3xl font-bold text-gray-700">{stats.thisMonth}</div>
          </div>
        </div>

        {/* 탭 네비게이션 */}
        <div className="flex gap-2 mb-6 border-b">
          <button
            onClick={() => setActiveTab('pending')}
            className={`px-6 py-3 font-semibold transition-colors ${
              activeTab === 'pending'
                ? 'text-yellow-600 border-b-2 border-yellow-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            승인 대기 ({pendingBookings.length})
          </button>
          <button
            onClick={() => setActiveTab('all')}
            className={`px-6 py-3 font-semibold transition-colors ${
              activeTab === 'all'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            전체 예약
          </button>
        </div>

        {/* 승인 대기 탭 */}
        {activeTab === 'pending' && (
          <div className="space-y-4">
            {pendingBookings.length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm border p-12 text-center text-gray-500">
                승인 대기중인 예약이 없습니다.
              </div>
            ) : (
              pendingBookings.map(booking => (
                <div key={booking.id} className="bg-white rounded-lg shadow-sm border p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-xl font-bold">{booking.facility}</h3>
                        {getStatusBadge(booking.status)}
                      </div>
                      <div className="grid grid-cols-2 gap-4 text-gray-600">
                        <div className="flex items-center gap-2">
                          <Calendar size={16} />
                          <span>{booking.date} {booking.time}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Users size={16} />
                          <span>{booking.participants}명</span>
                        </div>
                        <div>
                          <span className="font-medium">신청자:</span> {booking.userName} ({booking.userCode})
                        </div>
                        <div>
                          <span className="font-medium">신청일시:</span> {booking.requestedAt}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => setSelectedBooking(booking)}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
                    >
                      처리하기
                    </button>
                  </div>
                  
                  <div className="border-t pt-4 space-y-2">
                    <div>
                      <span className="text-sm font-medium text-gray-600">사용 목적:</span>
                      <p className="text-gray-800 mt-1">{booking.purpose}</p>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-600">요청 장비:</span>
                      <p className="text-gray-800 mt-1">{booking.equipment}</p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* 전체 예약 탭 */}
        {activeTab === 'all' && (
          <div>
            {/* 필터 */}
            <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">상태</label>
                  <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500"
                  >
                    {statuses.map(status => (
                      <option key={status.value} value={status.value}>{status.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">시설</label>
                  <select
                    value={filterFacility}
                    onChange={(e) => setFilterFacility(e.target.value)}
                    className="w-full border rounded-lg p-2 focus:ring-2 focus:ring-blue-500"
                  >
                    {facilities.map(facility => (
                      <option key={facility.value} value={facility.value}>{facility.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">검색</label>
                  <div className="relative">
                    <input
                      type="text"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      placeholder="이름, 학번, 시설명"
                      className="w-full border rounded-lg p-2 pl-10 focus:ring-2 focus:ring-blue-500"
                    />
                    <Search className="absolute left-3 top-2.5 text-gray-400" size={18} />
                  </div>
                </div>
              </div>
            </div>

            {/* 예약 목록 테이블 */}
            <div className="bg-white rounded-lg shadow-sm border overflow-hidden">
              <table className="w-full">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">신청일시</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">시설</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">예약일시</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">신청자</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">인원</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">상태</th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-gray-700">액션</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {filteredBookings.map(booking => (
                    <tr key={booking.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-sm text-gray-600">{booking.requestedAt}</td>
                      <td className="px-6 py-4 text-sm font-medium">{booking.facility}</td>
                      <td className="px-6 py-4 text-sm text-gray-600">{booking.date} {booking.time}</td>
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {booking.userName}<br/>
                        <span className="text-xs text-gray-500">{booking.userCode}</span>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600">{booking.participants}명</td>
                      <td className="px-6 py-4">{getStatusBadge(booking.status)}</td>
                      <td className="px-6 py-4">
                        <button
                          onClick={() => setSelectedBooking(booking)}
                          className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                        >
                          상세보기
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredBookings.length === 0 && (
                <div className="p-12 text-center text-gray-500">
                  검색 결과가 없습니다.
                </div>
              )}
            </div>
          </div>
        )}

        {/* 상세보기/처리 모달 */}
        {selectedBooking && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
              <div className="p-6 border-b">
                <div className="flex items-center justify-between mb-2">
                  <h2 className="text-2xl font-bold">예약 상세</h2>
                  <button
                    onClick={() => {
                      setSelectedBooking(null);
                      setApprovalNote('');
                      setRejectionReason('');
                    }}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    ✕
                  </button>
                </div>
                {getStatusBadge(selectedBooking.status)}
              </div>

              <div className="p-6 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">시설</div>
                    <div className="text-gray-800 font-semibold">{selectedBooking.facility}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">예약 일시</div>
                    <div className="text-gray-800">{selectedBooking.date} {selectedBooking.time}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">신청자</div>
                    <div className="text-gray-800">{selectedBooking.userName} ({selectedBooking.userCode})</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">이메일</div>
                    <div className="text-gray-800">{selectedBooking.userEmail}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">예상 인원</div>
                    <div className="text-gray-800">{selectedBooking.participants}명</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium text-gray-600 mb-1">신청 일시</div>
                    <div className="text-gray-800">{selectedBooking.requestedAt}</div>
                  </div>
                </div>

                <div>
                  <div className="text-sm font-medium text-gray-600 mb-1">사용 목적</div>
                  <div className="text-gray-800 bg-gray-50 p-3 rounded-lg">{selectedBooking.purpose}</div>
                </div>

                <div>
                  <div className="text-sm font-medium text-gray-600 mb-1">요청 장비</div>
                  <div className="text-gray-800 bg-gray-50 p-3 rounded-lg">{selectedBooking.equipment}</div>
                </div>

                {selectedBooking.status === 'approved' && selectedBooking.adminNote && (
                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <div className="text-sm font-medium text-blue-800 mb-1">관리자 비고</div>
                    <div className="text-sm text-blue-700">{selectedBooking.adminNote}</div>
                  </div>
                )}

                {selectedBooking.status === 'rejected' && selectedBooking.rejectionReason && (
                  <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                    <div className="text-sm font-medium text-red-800 mb-1">반려 사유</div>
                    <div className="text-sm text-red-700">{selectedBooking.rejectionReason}</div>
                  </div>
                )}

                {selectedBooking.status === 'pending' && (
                  <div className="space-y-4 border-t pt-4">
                    <div>
                      <label className="block text-sm font-medium mb-2">승인 비고 (선택)</label>
                      <textarea
                        value={approvalNote}
                        onChange={(e) => setApprovalNote(e.target.value)}
                        placeholder="승인 시 전달할 메시지를 입력하세요 (예: 장비 세팅 완료)"
                        className="w-full border rounded-lg p-3 h-24 focus:ring-2 focus:ring-blue-500"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium mb-2">반려 사유 (필수)</label>
                      <textarea
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="반려 시 사유를 입력하세요"
                        className="w-full border rounded-lg p-3 h-24 focus:ring-2 focus:ring-red-500"
                      />
                    </div>

                    <div className="flex gap-3">
                      <button
                        onClick={() => handleApprove(selectedBooking.id)}
                        className="flex-1 bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition-colors flex items-center justify-center gap-2"
                      >
                        <CheckCircle size={20} />
                        승인
                      </button>
                      <button
                        onClick={() => handleReject(selectedBooking.id)}
                        className="flex-1 bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700 transition-colors flex items-center justify-center gap-2"
                      >
                        <XCircle size={20} />
                        반려
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminBookingSystem;