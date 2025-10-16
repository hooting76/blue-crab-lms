# 🎯 API �E�����E��E� �E����E���E�� (v4.0 - POST ?? ??)

## 📋 �E����E�E�E��된 �E�����E��E��E�

### **✁E�E�의 �E��E� (Lecture �������E�)**
| �E�����E��E� | �E�드����E���� ������ | �E��E�E�E��E� | �E�E�E |
|---------|---------------|---------|------|
| `LectureController` | `/api/lectures` | �E�의 CRUD, ����E�E **�E�각E�E��E� �E�의 �E����E* | ✁E�E�E��E|
| `EnrollmentController` | `/api/enrollments` | �E�강신�E�/�E��E�E �E��E�E���E� | ✁E�E�E��E|
| `AssignmentController` | `/api/assignments` | �E��E�ECRUD, �E�춁E�E�E��E| ✁E�E�E��E|
| `ProfessorAttendanceController` | `/api/professor/attendance` | �E��E �E��E �E��E� | ✁E�E�E��E|
| `StudentAttendanceController` | `/api/student/attendance` | ����E �E��E �E�청 | ✁E�E�E��E|

### **�E �E��E�E�E�가�E�E�E�드����E����**
| �E�드����E���� | �E��E�E�E| �E��E� | �E�����E��E� |
|-----------|-------|------|---------|
| `/lectures/eligible` | POST (Body: {studentIdx}) | ����E�E�E�E�각E�E��E� �E�의 �E����E(0�E�E�E�칁E | LectureController |

### **✁E�E��E�E�E��E�**
| �E�����E��E� | �E�드����E���� ������ | �E��E�E�E��E� | �E�E�E |
|---------|---------------|---------|------|
| `AuthController` | `/api/auth` | �E�그�E�/�������E��E� | ✁E�E�E��E|
| `AdminController` | `/api/admin` | �E��E��E�E2�E� �E��E�E| ✁E�E�E��E|
| `AdminAuthTokenController` | `/api/admin/auth` | �E��E��E�E������ �E��E� | ✁E�E�E��E|

### **✁E�E�시���E�E��E�**
| �E�����E��E� | �E�드����E���� ������ | �E��E�E�E��E� | �E�E�E |
|---------|---------------|---------|------|
| `BoardController` | `/api/boards` | �E�시���E�E����E| ✁E�E�E��E|
| `BoardCreateController` | `/api/boards` | �E�시�E� �E�성 | ✁E�E�E��E|
| `BoardUpdateController` | `/api/boards` | �E�시�E� �E�적E| ✁E�E�E��E|
| `BoardAttachmentUploadController` | `/api/board-attachments` | ���일 �E�E��드 | ✁E�E�E��E|
| `BoardAttachmentDownloadController` | `/api/board-attachments` | ���일 �E��E��E�드 | ✁E�E�E��E|

## ❁E�E��E����E�E��E��E�

### **�E�의 ���가 �E�스���E*
- `EvaluationController` - �E��E����지 �E�음
- `LectureEvaluationController` - �E��E����지 �E�음
- **���E�� �E�드����E����**: `/api/evaluations/*`
- **�E��E�����E��E��E�**: `UserTbl.lectureEvaluations` ���E���E�E�E��E�

### **�E��E�각E�E�E�� �E�스���E*
- `ChatController` - �E��E����지 �E�음
- **���E�� �E�드����E����**: `/api/chat/*`
- **�E����E��**: WebSocket �E��E�E���E��

### **�E��E��E�E����E�E�E�E�E��E�터�E�E*
- `StatisticsController` - �E��E����지 �E�음
- **���E�� �E�드����E����**: `/api/admin/statistics/*`
- **���E��**: �E�볁E�E�����E��E��E��E �E��E� ����E�E��E�E�공

## 🔧 �E�드����E���� ������ �E�약

### **�E��E�E�E����E�� ������**
```
/api/auth/*              - �E��E�E(AuthController)
/api/admin/*             - �E��E��E�E(AdminController, AdminAuthTokenController)
/api/lectures/*          - �E�의 �E��E� (LectureController)
  └─ /api/lectures/eligible/{studentId} - �E �E�각E�E��E� �E�의 �E����E
/api/enrollments/*       - �E�강신�E� (EnrollmentController)
/api/assignments/*       - �E��E�E�E��E� (AssignmentController)
/api/professor/attendance/* - �E��E �E��E (ProfessorAttendanceController)
/api/student/attendance/*   - ����E �E��E (StudentAttendanceController)
/api/boards/*            - �E�시���E(Board*Controller�E�)
/api/board-attachments/* - ���일 �E��E� (BoardAttachment*Controller�E�)
```

### **�E��E�에�E�E�E�거�E�E������ (�E��E����E**
```
/api/evaluations/*       - �E�의 ���가 (�E��E����E
/api/chat/*              - �E�E�� (�E��E����E
/api/admin/statistics/*  - �E��E��E�E����E�E(�E��E����E
```

## 📊 �E����E���E�� ����E�E

- **�E����E�E�E��E*: 13�E�E�E�����E��E�
- **�E��E����E*: 3�E�E�E��E�E�E��E� �E�E��
- **API �E��E�E�E��E�율**: 100% (�E��E�E�E����E�� �E�E��E�E��E�E
- **�E�드����E���� ������ ����E��E�**: ✁E�E�E��E

---

*�E� �E��E�는 API �E�E���E�E�E��E��E� �E���� �E��E�에�E�E�E�성�E�었�E��E�다.*
