import React, { useState } from 'react';
import '../../../css/Introductions/BlueCrabHistory.css';

export default function BlueCrabHistory() {
    const [expandedDecade, setExpandedDecade] = useState('2020');
    
    const historyData = [
        {
            decade: '1970',
            title: '대학 설립기',
            years: [
                {
                    year: '1970',
                    events: [
                        { month: '3월', title: '한국종합대학교 설립 인가' },
                        { month: '4월', title: '초대 총장 취임' },
                        { month: '9월', title: '제1회 입학식 거행',
                            details: ['컴퓨터공학과(정원 50명)', '기계공학과(정원 40명)', '국어국문학과(정원 30명)', '경영학과(정원 40명)']
                        }
                    ]
                },
                {
                    year: '1972',
                    events: [
                        { month: '3월', title: '전자공학과(정원 30명), 경제학과(정원 30명) 신설' },
                        { month: '9월', title: '본관 및 강의동 준공' }
                    ]
                },
                {
                    year: '1974',
                    events: [
                        { month: '3월', title: '물리학과(정원 30명), 수학과(정원 20명) 신설' },
                        { month: '11월', title: '제1회 졸업식 (졸업생 152명)' }
                    ]
                },
                {
                    year: '1976',
                    events: [
                        { month: '3월', title: '영어영문학과(정원 35명), 역사학과(정원 25명) 신설' },
                        { month: '6월', title: '도서관 신축 (건평 800평)' }
                    ]
                },
                {
                    year: '1978',
                    events: [
                        { month: '3월', title: '정치외교학과(정원 25명), 분자화학과(정원 30명) 신설' },
                        { month: '9월', title: '인문관 및 자연과학관 준공' }
                    ]
                }
            ]
        },
        {
            decade: '1980',
            title: '성장과 확장',
            years: [
                {
                    year: '1980',
                    events: [
                        { month: '3월', title: '간호학과(정원 50명) 신설' },
                        { month: '5월', title: '대학원 설립 인가 (석사과정)' }
                    ]
                },
                {
                    year: '1982',
                    events: [
                        { month: '3월', title: '항해학과(정원 30명), 조선학과(정원 40명) 신설' },
                        { month: '9월', title: '해양관 준공' },
                        { month: '12월', title: '재학생 2,000명 돌파' }
                    ]
                },
                {
                    year: '1985',
                    events: [
                        { month: '3월', title: '해양수산학과(정원 25명), 도선학과(정원 30명) 신설' },
                        { month: '6월', title: '해양과학연구소 설립' }
                    ]
                },
                {
                    year: '1987',
                    events: [
                        { month: '3월', title: '치위생학과(정원 40명), 약학과(정원 50명) 신설' },
                        { month: '9월', title: '보건관 준공' }
                    ]
                },
                {
                    year: '1989',
                    events: [
                        { month: '3월', title: '해양경찰학과(정원 40명) 신설' },
                        { month: '11월', title: '경찰청과 위탁교육 협약 체결' },
                        { month: '12월', title: '재학생 3,500명 돌파' }
                    ]
                }
            ]
        },
        {
            decade: '1990',
            title: '특성화 발전기',
            years: [
                {
                    year: '1992',
                    events: [
                        { month: '3월', title: '보건정책학과(정원 35명) 신설' },
                        { month: '9월', title: '부속병원 개원' }
                    ]
                },
                {
                    year: '1995',
                    events: [
                        { month: '3월', title: '해군사관학과(정원 80명) 신설' },
                        { month: '5월', title: '국방부와 위탁교육 협약 체결' },
                        { month: '9월', title: '군사학관 준공' }
                    ]
                },
                {
                    year: '1998',
                    events: [
                        { month: '3월', title: 'ICT융합학과(정원 60명) 신설' },
                        { month: '10월', title: '정보통신관 준공' },
                        { month: '12월', title: '재학생 5,000명 돌파' }
                    ]
                }
            ]
        },
        {
            decade: '2000',
            title: '종합 발전 및 체제 개편',
            years: [
                {
                    year: '2002',
                    events: [
                        { month: '3월', title: '컴퓨터공학과 정원 80명으로 확대' },
                        { month: '6월', title: '산학협력단 설립' }
                    ]
                },
                {
                    year: '2005',
                    events: [
                        { 
                            month: '9월', 
                            title: '학부제 전면 개편 (5개 학부 체제 출범)',
                            details: [
                                '공학부 (컴퓨터공학, 기계공학, 전자공학, ICT융합)',
                                '인문학부 (국어국문, 역사학, 경영, 경제, 정치외교, 영어영문)',
                                '자연과학부 (물리학, 수학, 분자화학)',
                                '보건학부 (간호학, 치위생, 약학, 보건정책학)',
                                '해양학부 (항해학, 해양경찰, 해군사관, 도선학, 해양수산학, 조선학)'
                            ]
                        }
                    ]
                },
                {
                    year: '2008',
                    events: [
                        { month: '3월', title: 'ICT융합학과 정원 100명으로 확대' },
                        { month: '5월', title: '교육역량강화사업 선정' },
                        { month: '11월', title: '재학생 7,000명 돌파' }
                    ]
                }
            ]
        },
        {
            decade: '2010',
            title: '글로벌 경쟁력 강화',
            years: [
                {
                    year: '2010',
                    events: [
                        { month: '3월', title: '컴퓨터공학과 정원 100명 달성' },
                        { month: '9월', title: '국제교류센터 설립' }
                    ]
                },
                {
                    year: '2012',
                    events: [
                        { month: '3월', title: '해군사관학과 정원 120명으로 확대' },
                        { month: '7월', title: '대학종합평가 A등급 획득' }
                    ]
                },
                {
                    year: '2015',
                    events: [
                        { month: '3월', title: '간호학과 정원 120명으로 확대' },
                        { month: '5월', title: '치위생학과, 약학과 각 80명으로 확대' },
                        { month: '9월', title: '창업지원센터 개관' }
                    ]
                },
                {
                    year: '2018',
                    events: [
                        { month: '3월', title: 'ICT융합학과 정원 125명으로 확대' },
                        { month: '6월', title: '해양경찰학과 정원 70명으로 확대' },
                        { month: '11월', title: '재학생 9,500명 돌파' }
                    ]
                }
            ]
        },
        {
            decade: '2020',
            title: '미래 교육 혁신',
            years: [
                {
                    year: '2020',
                    events: [
                        { month: '3월', title: '보건정책학과 정원 60명으로 확대' },
                        { month: '9월', title: '스마트캠퍼스 구축 완료' },
                        { month: '12월', title: 'AI융합교육센터 설립' }
                    ]
                },
                {
                    year: '2022',
                    events: [
                        { month: '3월', title: '항해학과 정원 50명, 도선학과 정원 50명으로 확대' },
                        { month: '6월', title: '조선학과 정원 80명으로 확대' },
                        { month: '9월', title: '해양수산학과 정원 40명으로 확대' }
                    ]
                },
                {
                    year: '2024',
                    events: [
                        { month: '3월', title: '철학과(정원 20명) 신설' },
                        { month: '5월', title: '인문학 르네상스 프로젝트 출범' },
                        { month: '9월', title: '기계공학과 정원 60명으로 확대' }
                    ]
                },
                {
                    year: '2025',
                    events: [
                        { month: '3월', title: '전체 학부 정원 조정 완료 (총 재학생 1,510명 규모)' },
                        { month: '5월', title: '대학 설립 55주년 기념식' },
                        { month: '9월', title: '미래교육혁신관 착공 예정' }
                    ]
                }
            ]
        }
    ];

    const achievements = [
        {
            category: '교육 부문',
            icon: '🎓',
            items: [
                '국가장학사업 우수대학 10년 연속 선정',
                '교육역량강화사업 15년 연속 선정',
                '대학혁신지원사업 5년 연속 선정'
            ]
        },
        {
            category: '연구 부문',
            icon: '🏆',
            items: [
                '연간 SCI급 논문 150편 이상 게재',
                '국가 R&D 과제 수행 누적 500건 이상',
                '기술이전 및 특허등록 누적 300건 이상'
            ]
        },
        {
            category: '산학협력',
            icon: '🏢',
            items: [
                '300여개 기업 및 기관과 MOU 체결',
                '취업률 최근 5년 평균 87% 달성',
                '창업기업 누적 150개사 배출'
            ]
        },
        {
            category: '국제화',
            icon: '🌍',
            items: [
                '35개국 150여개 대학과 교류 협정',
                '외국인 유학생 500명 이상',
                '해외 파견 학생 연간 200명 이상'
            ]
        }
    ];

    const toggleDecade = (decade) => {
        setExpandedDecade(expandedDecade === decade ? null : decade);
    };
    
    return (
        <div className="history-container">
            <div className="history-wrapper">
                <div className="timeline-container">
                    {historyData.map((period) => (
                        <div key={period.decade} className="timeline-item">
                            <button
                                onClick={() => toggleDecade(period.decade)}
                                className="timeline-header"
                            >
                                <div className="timeline-header-content">
                                    <span className="timeline-icon">📅</span>
                                    <div className="timeline-header-text">
                                        <div className="timeline-decade">{period.decade}
                                            <span>년대</span>
                                        </div>
                                        <div className="timeline-title">
                                            <p>{period.title}</p>
                                        </div>
                                    </div>
                                </div>
                                <span className="timeline-toggle">
                                    {expandedDecade === period.decade ? '▲' : '▼'}
                                </span>
                            </button>

                        {expandedDecade === period.decade && (
                            <div className="timeline-content">
                                {period.years.map((yearData) => (
                                    <div key={yearData.year} className="year-section">
                                        <h3 className="year-title">
                                            {yearData.year}년
                                        </h3>
                                        <div className="events-container">
                                            {yearData.events.map((event, idx) => (
                                                <div key={idx} className="event-item">
                                                    <div className="event-content">
                                                        <span className="event-month">
                                                            {event.month}
                                                        </span>
                                                        <div className="event-details">
                                                            <p className="event-title">
                                                                {event.title}
                                                            </p>
                                                            {event.details && (
                                                                <ul className="event-details-list">
                                                                    {event.details.map((detail, detailIdx) => (
                                                                        <li key={detailIdx}>{detail}</li>
                                                                    ))}
                                                                </ul>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                        </div>
                    ))}
                </div>

                <div className="achievements-section">
                    <h2 className="achievements-title">
                        주요 성과
                    </h2>
                    <div className="achievements-grid">
                        {achievements.map((achievement, idx) => (
                            <div key={idx} className="achievement-card">
                                <div className="achievement-header">
                                    <span className="achievement-icon">
                                        {achievement.icon}
                                    </span>
                                    <h3 className="achievement-category">
                                        {achievement.category}
                                    </h3>
                                </div>
                                <ul className="achievement-list">
                                    {achievement.items.map((item, itemIdx) => (
                                        <li key={itemIdx}>
                                            <span className="bullet">•</span>
                                            <span>{item}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>

                    <div className="special-achievement">
                        <h3 className="special-title">
                            특성화 분야
                        </h3>
                        <div className="special-grid">
                            <div className="special-item">
                                <p>해양분야 전문인력 양성 국내 1위</p>
                            </div>
                            <div className="special-item">
                                <p>국방·경찰 분야 위탁교육 최우수 평가</p>
                            </div>
                            <div className="special-item">
                                <p>보건의료 분야 지역거점대학 선정</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};