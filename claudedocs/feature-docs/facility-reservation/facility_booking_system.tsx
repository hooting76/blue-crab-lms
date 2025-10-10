import React, { useState } from 'react';
import { Calendar, Clock, MapPin, Users, AlertCircle, CheckCircle } from 'lucide-react';

const FacilityBookingSystem = () => {
  const [currentPage, setCurrentPage] = useState('booking');
  const [selectedFacility, setSelectedFacility] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [bookingStatusFilter, setBookingStatusFilter] = useState('');
  const [activeTab, setActiveTab] = useState('ongoing');
  const [selectedDate, setSelectedDate] = useState('2025-10-15');
  const [selectedTimes, setSelectedTimes] = useState([]);
  const [bookingPurpose, setBookingPurpose] = useState('');
  const [participantCount, setParticipantCount] = useState(1);
  const [requestedEquipment, setRequestedEquipment] = useState('');
  const [expandedBooking, setExpandedBooking] = useState(null);

  const facilities = [
    { id: 1, name: '스터디룸 201호', capacity: 4, location: '도서관동 1층', category: 'study', equipment: '화이트보드, 빔프로젝터' },
    { id: 2, name: '스터디룸 202호', capacity: 4, location: '도서관동 1층', category: 'study', equipment: '화이트보드' },
    { id: 3, name: '스터디룸 203호', capacity: 4, location: '도서관동 2층', category: 'study', equipment: '화이트보드, TV' },
    { id: 4, name: '스터디룸 204호', capacity: 4, location: '도서관동 2층', category: 'study', equipment: '화이트보드, 빔프로젝터' },
    { id: 5, name: '세미나실 301호', capacity: 4, location: '본관 3층', category: 'seminar', equipment: '빔프로젝터, 음향시설, 마이크' },
    { id: 6, name: '세미나실 302호', capacity: 4, location: '본관 3층', category: 'seminar', equipment: '빔프로젝터, 화이트보드' },
    { id: 7, name: '세미나실 303호', capacity: 4, location: '본관 4층', category: 'seminar', equipment: '대형스크린, 음향시설' },
    { id: 8, name: '대강당', capacity: 8, location: '본관 2층', category: 'auditorium', equipment: '무대조명, 음향시설, 빔프로젝터' },
    { id: 9, name: '체육관', capacity: 6, location: '체육관동 1층', category: 'gym', highlight: true, equipment: '농구골대, 음향시설' },
    { id: 10, name: '소강당', capacity: 6, location: '본관 1층', category: 'auditorium', equipment: '빔프로젝터, 음향시설' }
  ];

  const categories = [
    { value: '', label: '전체' },
    { value: 'study', label: '스터디룸' },
    { value: 'seminar', label: '세미나실' },
    { value: 'auditorium', label: '강당' },
    { value: 'gym', label: '체육시설' }
  ];

  const bookingStatuses = [
    { value: '', label: '전체' },
    { value: 'approved', label: '승인 완료' },
    { value: 'pending', label: '승인 대기' },
    { value: 'completed', label: '이용 완료' },
    { value: 'rejected', label: '반려됨' }
  ];

  const filteredFacilities = selectedCategory 
    ? facilities.filter(f => f.category === selectedCategory)
    : facilities;

  const timeSlots = [
    { time: '09:00', status: 'available' },
    { time: '10:00', status: 'available' },
    { time: '11:00', status: 'available' },
    { time: '12:00', status: 'available' },
    { time: '13:00', status: 'reserved' },
    { time: '14:00', status: 'reserved' },
    { time: '15:00', status: 'available' },
    { time: '16:00', status: 'available' },
    { time: '17:00', status: 'available' },
    { time: '18:00', status: 'pending' },
    { time: '19:00', status: 'available' },
    { time: '20:00', status: 'available' }
  ];

  const myBookings = [
    { 
      id: 1, 
      facility: '세미나실 301호', 
      date: '2025-10-05', 
      time: '14:00-16:00', 
      status: 'approved', 
      purpose: '컴퓨터공학과 캡스톤 팀 프로젝트 회의', 
      participants: 8, 
      equipment: '빔프로젝터, 음향시설', 
      adminNote: '행사 진행에 필요한 장비 세팅 완료하였습니다.' 
    },
    { 
      id: 2, 
      facility: '스터디룸 201호', 
      date: '2025-10-08', 
      time: '10:00-12:00', 
      status: 'pending', 
      purpose: '중간고사 스터디', 
      participants: 4, 
      equipment: '화이트보드' 
    },
    { 
      id: 3, 
      facility: '대강당의실 401호', 
      date: '2025-09-28', 
      time: '15:00-17:00', 
      status: 'completed', 
      purpose: '학과 세미나', 
      participants: 50, 
      equipment: '빔프로젝터, 음향시설, 마이크' 
    },
    { 
      id: 4, 
      facility: '체육관', 
      date: '2025-10-12', 
      time: '18:00-20:00', 
      status: 'rejected', 
      reason: '해당 시간대는 정기 행사로 사용 불가합니다.', 
      purpose: '농구 동아리 연습', 
      participants: 15, 
      equipment: '농구골대, 음향시설' 
    }
  ];

  const ongoingBookings = myBookings.filter(b => b.status === 'approved' || b.status === 'pending');
  const completedBookings = myBookings.filter(b => b.status === 'completed' || b.status === 'rejected');

  const getFilteredBookings = (bookings) => {
    if (!bookingStatusFilter) return bookings;
    return bookings.filter(b => b.status === bookingStatusFilter);
  };

  const displayedBookings = activeTab === 'ongoing' 
    ? getFilteredBookings(ongoingBookings) 
    : getFilteredBookings(completedBookings);

  const handleTimeClick = (time, status) => {
    if (status !== 'available') return;
    
    if (selectedTimes.includes(time)) {
      setSelectedTimes(selectedTimes.filter(t => t !== time));
    } else {
      setSelectedTimes([...selectedTimes, time]);
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'available': return 'bg-green-50 border-green-200 text-green-700';
      case 'selected': return 'bg-blue-500 text-white border-blue-600';
      case 'reserved': return 'bg-red-50 border-red-200 text-red-700';
      case 'pending': return 'bg-yellow-50 border-yellow-200 text-yellow-700';
      default: return 'bg-gray-50 border-gray-200';
    }
  };

  const getStatusText = (status) => {
    switch(status) {
      case 'available': return '예약 가능';
      case 'selected': return '✓ 선택됨';
      case 'reserved': return '✕ 예약됨';
      case 'pending': return '⏱ 승인 대기';
      default: return '';
    }
  };

  const handleSubmit = () => {
    if (selectedTimes.length === 0) {
      alert('시간을 선택해주세요.');
      return;
    }
    if (!bookingPurpose.trim()) {
      alert('사용 목적을 입력해주세요.');
      return;
    }
    if (!requestedEquipment.trim()) {
      alert('사용할 장비를 입력해주세요.');
      return;
    }
    alert('예약이 신청되었습니다!');
    setSelectedTimes([]);
    setBookingPurpose('');
    setParticipantCount(1);
    setRequestedEquipment('');
  };

  const handleCancelBooking = (bookingId) => {
    if (window.confirm('예약을 취소하시겠습니까?')) {
      alert('예약이 취소되었습니다.');
    }
  };

  const toggleBookingDetail = (bookingId) => {
    setExpandedBooking(expandedBooking === bookingId ? null : bookingId);
  };

  if (currentPage === 'myBookings') {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-4xl mx-auto">
          <button 
            onClick={() => setCurrentPage('booking')}
            className="mb-6 text-blue-600 hover:text-blue-700 flex items-center gap-2"
          >
            ← 시설 목록으로 돌아가기
          </button>
          
          <h1 className="text-3xl font-bold mb-8">내 예약 현황</h1>
          
          <div className="mb-6 bg-white rounded-lg shadow-sm border p-4">
            <label className="block text-sm font-medium mb-2">예약 상태별 보기</label>
            <select 
              value={bookingStatusFilter}
              onChange={(e) => setBookingStatusFilter(e.target.value)}
              className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {bookingStatuses.map(status => (
                <option key={status.value} value={status.value}>{status.label}</option>
              ))}
            </select>
          </div>

          <div className="flex gap-2 mb-6 border-b">
            <button
              onClick={() => setActiveTab('ongoing')}
              className={`px-6 py-3 font-semibold transition-colors ${
                activeTab === 'ongoing'
                  ? 'text-blue-600 border-b-2 border-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              진행중 ({ongoingBookings.length})
            </button>
            <button
              onClick={() => setActiveTab('completed')}
              className={`px-6 py-3 font-semibold transition-colors ${
                activeTab === 'completed'
                  ? 'text-blue-600 border-b-2 border-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              완료 ({completedBookings.length})
            </button>
          </div>

          <div className="space-y-4">
            {displayedBookings.length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm border p-12 text-center text-gray-500">
                {bookingStatusFilter 
                  ? '해당 상태의 예약이 없습니다.'
                  : activeTab === 'ongoing' 
                    ? '진행중인 예약이 없습니다.' 
                    : '완료된 예약이 없습니다.'}
              </div>
            ) : (
              displayedBookings.map(booking => (
                <div key={booking.id} className="bg-white rounded-lg shadow-sm border overflow-hidden">
                  <button
                    onClick={() => toggleBookingDetail(booking.id)}
                    className="w-full p-6 text-left hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <h3 className="text-xl font-semibold mb-2">{booking.facility}</h3>
                        <div className="flex gap-4 text-gray-600">
                          <div className="flex items-center gap-2">
                            <Calendar size={16} />
                            <span>{booking.date}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Clock size={16} />
                            <span>{booking.time}</span>
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {booking.status === 'approved' && (
                          <span className="px-4 py-2 bg-green-100 text-green-700 rounded-full flex items-center gap-2">
                            <CheckCircle size={16} />
                            승인 완료
                          </span>
                        )}
                        {booking.status === 'pending' && (
                          <span className="px-4 py-2 bg-yellow-100 text-yellow-700 rounded-full flex items-center gap-2">
                            <Clock size={16} />
                            승인 대기
                          </span>
                        )}
                        {booking.status === 'completed' && (
                          <span className="px-4 py-2 bg-gray-100 text-gray-700 rounded-full">
                            이용 완료
                          </span>
                        )}
                        {booking.status === 'rejected' && (
                          <span className="px-4 py-2 bg-red-100 text-red-700 rounded-full flex items-center gap-2">
                            <AlertCircle size={16} />
                            반려됨
                          </span>
                        )}
                        <span className="text-gray-400">
                          {expandedBooking === booking.id ? '▲' : '▼'}
                        </span>
                      </div>
                    </div>
                  </button>
                  
                  {expandedBooking === booking.id && (
                    <div className="px-6 pb-6 border-t bg-gray-50">
                      <div className="pt-4 space-y-3">
                        <div>
                          <div className="text-sm font-medium text-gray-600 mb-1">사용 목적</div>
                          <div className="text-gray-800">{booking.purpose}</div>
                        </div>
                        <div>
                          <div className="text-sm font-medium text-gray-600 mb-1">예상 인원</div>
                          <div className="text-gray-800 flex items-center gap-2">
                            <Users size={16} />
                            {booking.participants}명
                          </div>
                        </div>
                        <div>
                          <div className="text-sm font-medium text-gray-600 mb-1">요청 장비</div>
                          <div className="text-gray-800">{booking.equipment}</div>
                        </div>
                        
                        {booking.status === 'approved' && booking.adminNote && (
                          <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                            <div className="text-sm font-medium text-blue-800 mb-1">관리자 비고</div>
                            <div className="text-sm text-blue-700">{booking.adminNote}</div>
                          </div>
                        )}
                        
                        {booking.status === 'rejected' && booking.reason && (
                          <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <div className="text-sm font-medium text-red-800 mb-1">반려 사유</div>
                            <div className="text-sm text-red-700">{booking.reason}</div>
                          </div>
                        )}
                        
                        {booking.status === 'pending' && (
                          <div className="mt-4 pt-4 border-t">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleCancelBooking(booking.id);
                              }}
                              className="w-full bg-red-500 text-white py-2 px-4 rounded-lg font-medium hover:bg-red-600 transition-colors"
                            >
                              예약 취소하기
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    );
  }

  if (selectedFacility) {
    const facility = facilities.find(f => f.id === selectedFacility);
    
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-4xl mx-auto">
          <button 
            onClick={() => setSelectedFacility(null)}
            className="mb-6 text-blue-600 hover:text-blue-700 flex items-center gap-2"
          >
            ← 시설 목록으로 돌아가기
          </button>
          
          <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-bold">{facility.name}</h2>
              <button 
                onClick={() => setCurrentPage('myBookings')}
                className="text-blue-600 hover:text-blue-700"
              >
                내 예약 현황 →
              </button>
            </div>
            <div className="space-y-2">
              <div className="flex gap-6 text-gray-600">
                <div className="flex items-center gap-2">
                  <MapPin size={18} />
                  <span>{facility.location}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Users size={18} />
                  <span>수용 인원: {facility.capacity * 50}명</span>
                </div>
              </div>
              <div className="text-gray-600">
                <span className="font-medium">보유 장비:</span> {facility.equipment}
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">예약 날짜 선택</h3>
            <input 
              type="date" 
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
            <h3 className="text-lg font-semibold mb-4">{selectedDate} 예약 현황</h3>
            
            <div className="flex gap-4 mb-4 text-sm">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-green-100 border border-green-300 rounded"></div>
                <span>예약 가능</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-blue-500 rounded"></div>
                <span>선택됨</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-red-100 border border-red-300 rounded"></div>
                <span>예약됨</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-yellow-100 border border-yellow-300 rounded"></div>
                <span>승인 대기</span>
              </div>
            </div>

            <div className="grid grid-cols-4 gap-4">
              {timeSlots.map(slot => {
                const isSelected = selectedTimes.includes(slot.time);
                const displayStatus = isSelected ? 'selected' : slot.status;
                
                return (
                  <button
                    key={slot.time}
                    onClick={() => handleTimeClick(slot.time, slot.status)}
                    disabled={slot.status !== 'available'}
                    className={`p-4 rounded-lg border-2 transition-all ${getStatusColor(displayStatus)} ${
                      slot.status === 'available' ? 'cursor-pointer hover:scale-105' : 'cursor-not-allowed opacity-75'
                    }`}
                  >
                    <div className="text-xl font-bold mb-1">{slot.time}</div>
                    <div className="text-sm">{getStatusText(displayStatus)}</div>
                    {isSelected && <div className="text-xs mt-1">{selectedTimes.indexOf(slot.time) + 1}번째</div>}
                  </button>
                );
              })}
            </div>

            {selectedTimes.length > 0 && (
              <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
                <div className="flex items-start gap-2">
                  <CheckCircle className="text-blue-600 mt-1" size={20} />
                  <div className="flex-1">
                    <div className="font-semibold text-blue-900 mb-2">선택한 시간</div>
                    <div className="space-y-1 text-blue-800">
                      {selectedTimes.sort().map((time, idx) => {
                        const endHour = parseInt(time.split(':')[0]) + 1;
                        return (
                          <div key={idx} className="flex items-center gap-2">
                            <Clock size={16} />
                            <span>{time} - {endHour.toString().padStart(2, '0')}:00 (1시간)</span>
                          </div>
                        );
                      })}
                    </div>
                    <div className="mt-2 text-sm text-blue-700">총 {selectedTimes.length}시간 선택</div>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-semibold mb-4">예약 신청</h3>
            
            <div className="mb-4">
              <label className="block text-sm font-medium mb-2">
                선택한 시간: <span className="text-green-600 font-semibold">
                  {selectedTimes.length > 0 ? selectedTimes.sort().join(', ') : '시간을 선택해주세요'}
                </span>
              </label>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium mb-2">
                사용 목적 <span className="text-red-500">*</span>
              </label>
              <textarea
                value={bookingPurpose}
                onChange={(e) => setBookingPurpose(e.target.value)}
                placeholder="예약 목적을 구체적으로 입력해주세요"
                className="w-full border rounded-lg p-3 h-24 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium mb-2">
                사용할 장비 <span className="text-red-500">*</span>
              </label>
              <textarea
                value={requestedEquipment}
                onChange={(e) => setRequestedEquipment(e.target.value)}
                placeholder="사용할 장비를 입력해주세요"
                className="w-full border rounded-lg p-3 h-20 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <p className="text-sm text-gray-500 mt-1">이 시설의 보유 장비: {facility.equipment}</p>
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium mb-2">
                예상 인원 <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                value={participantCount}
                onChange={(e) => setParticipantCount(e.target.value)}
                min="1"
                max={facility.capacity * 50}
                className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <p className="text-sm text-gray-500 mt-1">이 시설은 최대 {facility.capacity * 50}명까지 수용 가능합니다</p>
            </div>

            <div className="flex gap-3">
              <button 
                onClick={handleSubmit}
                className="flex-1 bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
              >
                예약 신청하기
              </button>
              <button 
                onClick={() => {
                  setSelectedTimes([]);
                  setBookingPurpose('');
                  setParticipantCount(1);
                  setRequestedEquipment('');
                }}
                className="px-6 py-3 border border-gray-300 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
              >
                취소
              </button>
            </div>
          </div>

          <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div className="flex items-start gap-2">
              <AlertCircle className="text-blue-600 mt-0.5" size={20} />
              <div className="text-sm text-blue-800">
                <strong>개인정보 보호:</strong> 예약된 시간대의 예약자 정보는 공개되지 않습니다. 
                예약 가능한 시간대를 선택하여 신청해주세요.
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">시설물 예약</h1>
          <button 
            onClick={() => setCurrentPage('myBookings')}
            className="text-blue-600 hover:text-blue-700 font-medium"
          >
            내 예약 현황 →
          </button>
        </div>
        <p className="text-gray-600 mb-8">원하는 시간을 클릭하여 선택하세요</p>

        <div className="mb-8">
          <h2 className="text-xl font-semibold mb-4">시설 선택</h2>
          
          <div className="mb-6">
            <label className="block text-sm font-medium mb-2">시설물 카테고리</label>
            <select 
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {categories.map(cat => (
                <option key={cat.value} value={cat.value}>{cat.label}</option>
              ))}
            </select>
          </div>

          <div className="space-y-4 max-h-[500px] overflow-y-auto pr-2">
            {filteredFacilities.map(facility => (
              <button
                key={facility.id}
                onClick={() => setSelectedFacility(facility.id)}
                className={`w-full text-left p-6 rounded-lg border-2 transition-all ${
                  facility.highlight 
                    ? 'border-yellow-400 bg-yellow-50 shadow-md' 
                    : 'border-gray-200 bg-white hover:border-blue-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-xl font-bold mb-2">{facility.name}</h3>
                    <div className="flex gap-4 text-gray-600 text-sm">
                      <span>최대 {facility.capacity}시간 • 중복 신청 가능</span>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm text-gray-500 mb-1">{facility.location}</div>
                    <div className="text-sm text-gray-600 flex items-center gap-1">
                      <Users size={16} />
                      수용 인원: {facility.capacity * 50}명
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border p-6">
          <h2 className="text-xl font-semibold mb-4">예약 날짜</h2>
          <input 
            type="date" 
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="w-full border rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>
    </div>
  );
};

export default FacilityBookingSystem;