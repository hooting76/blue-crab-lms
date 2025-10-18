# Backend API Catalog

## AdminAuthTokenController
- POST `/api/admin/auth/refresh` - refreshAdminToken

## AdminController
- POST `/api/admin/login` - adminLogin
- GET `/api/admin/verify-email` - verifyEmail

## AdminEmailAuthController
- POST `/api/admin/email-auth/request` - requestAdminEmailAuthCode
- POST `/api/admin/email-auth/verify` - verifyAdminEmailAuthCode

## AdminFacilityReservationController
- POST `/api/admin/reservations/all` - getAllReservations
- POST `/api/admin/reservations/approve` - approveReservation
- POST `/api/admin/reservations/pending` - getPendingReservations
- POST `/api/admin/reservations/pending/count` - getPendingCount
- POST `/api/admin/reservations/reject` - rejectReservation
- POST `/api/admin/reservations/search` - searchReservations
- POST `/api/admin/reservations/stats` - getReservationStats
- POST `/api/admin/reservations/{reservationIdx}` - getReservationDetail

## ApiController
- GET `/api/health` - health
- GET `/api/ping` - ping
- GET `/api/system-info` - systemInfo
- GET `/api/test` - test
- GET `/api/test-auth` - testAuth

## AssignmentController
- POST `/api/assignments` - createAssignment
- POST `/api/assignments/data` - getAssignmentData
- POST `/api/assignments/detail` - getAssignmentById
- POST `/api/assignments/list` - getAssignments
- POST `/api/assignments/submissions` - getSubmissions
- DELETE `/api/assignments/{assignmentIdx}` - deleteAssignment
- PUT `/api/assignments/{assignmentIdx}` - updateAssignment
- PUT `/api/assignments/{assignmentIdx}/grade` - gradeAssignment
- POST `/api/assignments/{assignmentIdx}/submit` - submitAssignment

## AuthController
- POST `/api/auth/login` - login
- POST `/api/auth/logout` - logout
- POST `/api/auth/refresh` - refreshToken
- GET `/api/auth/validate` - validateToken

## BoardAttachmentDeleteController
- POST `/api/board-attachments/delete` - deleteAttachments
- POST `/api/board-attachments/delete-all/{boardIdx}` - deleteAllAttachmentsByBoard

## BoardAttachmentDownloadController
- POST `/api/board-attachments/download` - downloadFile
- POST `/api/board-attachments/download/health` - healthCheck
- POST `/api/board-attachments/info` - getFileInfo

## BoardAttachmentUploadController
- POST `/api/board-attachments/upload/{boardIdx}` - uploadFiles

## BoardController
- POST `/api/boards/bycode` - getBoardsByCode
- POST `/api/boards/detail` - getBoardDetail
- POST `/api/boards/link-attachments/{boardIdx}` - linkAttachments
- POST `/api/boards/list` - getAllBoards

## BoardCreateController
- POST `/api/boards/create` - createBoard

## BoardStatisticsController
- POST `/api/boards/count` - getActiveBoardCount
- POST `/api/boards/count/bycode` - getBoardCountByCode
- POST `/api/boards/exists` - isBoardExists

## BoardUpdateController
- POST `/api/boards/delete/{boardIdx}` - deleteBoard
- POST `/api/boards/update/{boardIdx}` - updateBoard

## DatabaseController
- GET `/api/database/info` - getDatabaseInfo
- GET `/api/database/query` - executeQuery
- GET `/api/database/tables` - getTables
- GET `/api/database/tables/{tableName}/columns` - getTableColumns
- GET `/api/database/tables/{tableName}/sample` - getTableSample

## DatabaseTestController
- GET `/api/test/initialize-seats` - initializeSeats
- GET `/api/test/lamp-table-simple` - getSimpleTableTest
- GET `/api/test/lamp-table-status` - getLampTableStatus
- GET `/api/test/reset-all-seats` - resetAllSeats

## EnrollmentController
- POST `/api/enrollments/data` - getEnrollmentData
- POST `/api/enrollments/detail` - getEnrollmentById
- POST `/api/enrollments/enroll` - enrollInLecture
- POST `/api/enrollments/list` - getEnrollments
- DELETE `/api/enrollments/{enrollmentIdx}` - cancelEnrollment
- PUT `/api/enrollments/{enrollmentIdx}/attendance` - updateAttendance
- PUT `/api/enrollments/{enrollmentIdx}/grade` - updateGrade

## FacilityController
- POST `/api/facilities` - getAllFacilities
- POST `/api/facilities/search` - searchFacilities
- POST `/api/facilities/type/{facilityType}` - getFacilitiesByType
- POST `/api/facilities/{facilityIdx}` - getFacilityById
- POST `/api/facilities/{facilityIdx}/availability` - checkAvailability
- POST `/api/facilities/{facilityIdx}/daily-schedule` - getDailySchedule

## FacilityReservationController
- POST `/api/reservations` - createReservation
- POST `/api/reservations/my` - getMyReservations
- POST `/api/reservations/my/status/{status}` - getMyReservationsByStatus
- DELETE `/api/reservations/{reservationIdx}` - cancelReservation
- POST `/api/reservations/{reservationIdx}` - getReservationById

## FcmTokenController
- POST `/api/fcm/register` - register
- POST `/api/fcm/register/force` - registerForce
- POST `/api/fcm/send` - send
- POST `/api/fcm/send/batch` - sendBatch
- POST `/api/fcm/send/broadcast` - sendBroadcast
- POST `/api/fcm/send/data-only` - sendDataOnlyByUser
- GET `/api/fcm/stats` - getStats
- POST `/api/fcm/tokens/by-user` - lookupTokens
- DELETE `/api/fcm/unregister` - unregister

## FindIdController
- POST `/api/account/FindId` - findId

## FirebaseTestController
- GET `/api/test/firebase-status` - checkFirebaseStatus
- POST `/api/test/send-data-only` - sendDataOnly
- POST `/api/test/send-test-push` - sendTestPush
- POST `/api/test/send-topic-test` - sendTopicTest
- GET `/api/test/vapid-key` - getVapidKey

## HomeController
- GET `/` - home

## LectureController
- POST `/api/lectures` - getLectures
- POST `/api/lectures/create` - createLecture
- POST `/api/lectures/delete` - deleteLecture
- POST `/api/lectures/detail` - getLectureById
- POST `/api/lectures/eligible` - getEligibleLectures
- POST `/api/lectures/stats` - getLectureStats
- POST `/api/lectures/update` - updateLecture

## LogMonitorController
- GET `/admin/logs/monitor` - logMonitorPage
- GET `/admin/logs/status` - getLogStatus
- GET `/admin/logs/stream` - streamLogs

## MailAuthCheckController
- GET `/api/auth/config` - getAuthConfig
- GET `/sendMail` - sendAuthCode
- POST `/verifyCode` - verifyAuthCode

## MetricsController
- GET `/admin/metrics/logs` - getLogMetrics
- GET `/admin/metrics/system` - getSystemMetrics

## PasswordResetController
- POST `/api/auth/password-reset/change-password` - changePassword
- GET `/api/auth/password-reset/rate-limit-status` - getRateLimitStatus
- POST `/api/auth/password-reset/send-email` - sendResetEmail
- POST `/api/auth/password-reset/verify-code` - verifyCode
- POST `/api/auth/password-reset/verify-identity` - verifyIdentity

## ProfessorAttendanceController
- POST `/api/professor/attendance/mark` - markAttendance
- POST `/api/professor/attendance/requests` - getRequests
- POST `/api/professor/attendance/requests/count` - getPendingCount
- PUT `/api/professor/attendance/requests/{requestIdx}/approve` - approveRequest
- PUT `/api/professor/attendance/requests/{requestIdx}/reject` - rejectRequest

## ProfileController
- POST `/api/profile/me` - getMyProfile
- POST `/api/profile/me/completeness` - checkProfileCompleteness
- POST `/api/profile/me/image` - getMyProfileImage
- POST `/api/profile/me/image/file` - getMyProfileImageFile

## PushNotificationController
- POST `/api/push/send` - sendPushNotification
- POST `/api/push/send-data-only` - sendDataOnlyNotification
- POST `/api/push/send-data-only-batch` - sendDataOnlyBatch
- POST `/api/push/send-to-topic` - sendPushNotificationToTopic
- GET `/api/push/vapid-key` - getVapidPublicKey

## ReadingRoomController
- POST `/api/reading-room/checkout` - checkoutSeat
- POST `/api/reading-room/my-reservation` - getMyReservation
- POST `/api/reading-room/reserve` - reserveSeat
- POST `/api/reading-room/status` - getStatus

## RegistryController
- POST `/api/registry/cert/issue` - issueCertificate
- POST `/api/registry/me` - getMyRegistry
- GET `/api/registry/me/exists` - checkRegistryExists

## StudentAttendanceController
- POST `/api/student/attendance/detail` - getMyAttendance
- POST `/api/student/attendance/request` - requestExcuse
- POST `/api/student/attendance/requests` - getMyRequests

## UserController
- GET `/api/users` - getAllUsers
- POST `/api/users` - createUser
- GET `/api/users/professors` - getProfessorUsers
- GET `/api/users/search` - searchUsers
- GET `/api/users/search-all` - searchAllUsers
- GET `/api/users/search-birth` - searchUsersByBirth
- GET `/api/users/stats` - getUserStats
- GET `/api/users/students` - getStudentUsers
- DELETE `/api/users/{id}` - deleteUser
- GET `/api/users/{id}` - getUserById
- PUT `/api/users/{id}` - updateUser
- PATCH `/api/users/{id}/toggle-role` - toggleUserRole
