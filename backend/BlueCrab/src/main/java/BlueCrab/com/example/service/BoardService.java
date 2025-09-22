// 작성자 : 성태준
// 게시글 서비스 인터페이스

package BlueCrab.com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ========== 임포트 구문 ==========

// ========== Java 표준 라이브러리 ==========
import java.time.LocalDateTime;
import java.util.Optional;

// ========== Spring Framework ==========
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// ========== 프로젝트 내부 클래스 ==========
import BlueCrab.com.example.entity.BoardTbl;
import BlueCrab.com.example.repository.BoardRepository;


@Service
@Transactional
public class BoardService {

    // 의존성 주입
    @Autowired
    private BoardRepository boardRepository;

    
}
