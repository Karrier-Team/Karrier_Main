package com.karrier.mentoring.controller;

import com.karrier.mentoring.dto.*;
import com.karrier.mentoring.entity.*;
import com.karrier.mentoring.repository.ProgramRepository;
import com.karrier.mentoring.repository.QuestionCommentRepository;
import com.karrier.mentoring.service.CommunityQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin("http://localhost:3000")
@RequestMapping("/community")
@RestController
@RequiredArgsConstructor
public class CommunityQuestionController {

    private final CommunityQuestionService communityQuestionService;

    private final QuestionCommentRepository questionCommentRepository;
    private final ProgramRepository programRepository;

    //전체 프로그램 리스트 띄우기
    @GetMapping("/questions")
    public ResponseEntity<Object> programList() {
        //구현 예정

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    //해당 프로그램 전체 질문 리스트 띄우기
    @GetMapping("/question")
    public ResponseEntity<Object> questionList(@RequestParam("programNo") long programNo) {

        List<QuestionListDto> questionList = communityQuestionService.findQuestionList(programNo);
        if (questionList == null) {//해당 프로그램에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        return ResponseEntity.status(HttpStatus.OK).body(questionList);
    }

    //해당 프로그램 전체 리뷰 리스트에서 검색할 경우 (질문제목, 질문내용, 닉네임)
    @GetMapping("/question/search")
    public ResponseEntity<Object> questionList(@RequestParam("programNo") long programNo, @RequestParam("category") String category, @RequestParam("keyword") String keyword) {

        if (programNo == 0 || category.isEmpty() || keyword.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        List<QuestionListDto> questionList = communityQuestionService.QuestionSearchList(programNo, category, keyword);
        if (questionList == null) {//해당 프로그램에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        return ResponseEntity.status(HttpStatus.OK).body(questionList);
    }

    //질문 등록 요청
    @PostMapping("/question/new")
    public ResponseEntity<Object> createQuestion(@Valid QuestionFormDto questionFormDto, BindingResult bindingResult) {
        //빈칸있을 경우
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        Question question = Question.createQuestion(questionFormDto, email);

        Question savedQuestion = communityQuestionService.saveQuestion(question);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuestion);
    }

    //해당 질문 세부내용 출력
    @GetMapping("/question/detail")
    public ResponseEntity<Object> questionList(@RequestParam("programNo") long programNo,@RequestParam("questionNo") long questionNo) {

        Question question = communityQuestionService.findQuestion(programNo, questionNo);
        if (question == null) { //해당 프로그램 번호와 리뷰 번호에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }

        QuestionDetailDto questionDetail = communityQuestionService.getQuestionDetail(programNo, questionNo);

        return ResponseEntity.status(HttpStatus.OK).body(questionDetail);
    }

    //질문 답변 등록 요청
    @PostMapping({"/question/answer/new", "/question/answer/modify"})
    public ResponseEntity<Object> createAnswer(@Valid QuestionAnswerFormDto questionAnswerFormDto, BindingResult bindingResult) {
        //빈칸있을 경우
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        Question question = communityQuestionService.findQuestion(questionAnswerFormDto.getProgramNo(), questionAnswerFormDto.getQuestionNo());//이전에 저장했던 후기 정보 가져오기

        if (question == null) { //해당 프로그램 번호와 리뷰 번호에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }

        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        Program program = programRepository.findByProgramNo(questionAnswerFormDto.getProgramNo());
        if (!program.getEmail().equals(email)) { // 해당 프로그램 멘토인지 확인
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not mentor error");
        }

        Question questionWithAnswer = Question.createAnswer(questionAnswerFormDto, question);//후기에 댓글 정보 추가하기
        Question updatedQuestion = communityQuestionService.updateQuestion(questionWithAnswer);//DB에 저장

        return ResponseEntity.status(HttpStatus.OK).body(updatedQuestion);
    }

    //질문 좋아요 요청시
    @PostMapping("/question/like")
    public ResponseEntity<Object> likeAnswer(@RequestParam("programNo") long programNo,@RequestParam("questionNo") long questionNo) {

        Question question = communityQuestionService.findQuestion(programNo, questionNo);
        if (question == null) { //해당 프로그램 번호와 질문 번호에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }

        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        QuestionLike questionLike = communityQuestionService.findQuestionLike(programNo, questionNo, email);
        if (questionLike != null) { //이미 좋아요 한 회원일 경우
            return ResponseEntity.status(HttpStatus.CONFLICT).body("already like error");
        }

        question.setQuestionLikeNo(question.getQuestionLikeNo()+1); // 좋아요 1 증가
        QuestionLike newQuestionLike = QuestionLike.createQuestionLike(programNo, questionNo, email);
        ArrayList<Object> objects = communityQuestionService.likeQuestion(question, newQuestionLike);

        return ResponseEntity.status(HttpStatus.OK).body(objects);
    }

    //답변 좋아요 요청시
    @PostMapping("/question/answer/like")
    public ResponseEntity<Object> likeQuestion(@RequestParam("programNo") long programNo,@RequestParam("questionNo") long questionNo) {

        Question question = communityQuestionService.findQuestion(programNo, questionNo);
        if (question == null) { //해당 프로그램 번호와 질문에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        if (question.getAnswerDate() == null) { //해당 프로그램 번호와 질문 번호에 해당하는 답변이 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no answer error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        AnswerLike answerLike = communityQuestionService.findAnswerLike(programNo, questionNo, email);
        if (answerLike != null) { //이미 좋아요 한 회원일 경우
            return ResponseEntity.status(HttpStatus.CONFLICT).body("already like error");
        }

        question.setAnswerLikeNo(question.getAnswerLikeNo()+1); // 좋아요 1 증가
        AnswerLike newAnswerLike = AnswerLike.createAnswerLike(programNo, questionNo, email);
        ArrayList<Object> objects = communityQuestionService.likeAnswer(question, newAnswerLike);

        return ResponseEntity.status(HttpStatus.OK).body(objects);
    }
    
    //질문 수정 요청시
    @PostMapping("/question/modify")
    public ResponseEntity<Object> modifyQuestion(@Valid QuestionFormDto questionFormDto, BindingResult bindingResult, @RequestParam("questionNo") long questionNo) {
        //빈칸있을 경우
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        Question question = communityQuestionService.findQuestion(questionFormDto.getProgramNo(), questionNo);
        if (question == null) { //해당 프로그램 번호와 질문 번호에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();
        if (!question.getEmail().equals(email)) { //작성자가 아닐 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not writer error");
        }
        Question updatedQuestion = Question.updateQuestion(questionFormDto, question);
        Question savedQuestion = communityQuestionService.updateQuestion(updatedQuestion);

        return ResponseEntity.status(HttpStatus.OK).body(savedQuestion);
    }

    //질문 삭제 요청시
    @PostMapping("/question/delete")
    public ResponseEntity<Object> deleteQuestion(@RequestParam("programNo") long programNo, @RequestParam("questionNo") long questionNo) {

        Question question = communityQuestionService.findQuestion(programNo, questionNo);
        if (question == null) { //해당 프로그램 번호와 질문 번호에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();
        if (!question.getEmail().equals(email)) { //작성자가 아닐 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not writer error");
        }
        communityQuestionService.deleteQuestion(programNo, questionNo);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    //답변 삭제 요청시
    @PostMapping("/question/answer/delete")
    public ResponseEntity<Object> deleteAnswer(@RequestParam("programNo") long programNo, @RequestParam("questionNo") long questionNo) {

        Question question = communityQuestionService.findQuestion(programNo, questionNo);
        if (question == null) { //해당 프로그램 번호와 질문에 해당하는 데이터가 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        if (question.getAnswerDate() == null) { //해당 프로그램 번호와 질문 번호에 해당하는 답변이 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no answer error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        String answerEmail = programRepository.findByProgramNo(programNo).getEmail();//질문 답변 단 사람 이메일 찾기
        if (!email.equals(answerEmail)) { //작성자가 아닐 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not writer error");
        }
        Question updatedQuestion = communityQuestionService.deleteAnswer(programNo, questionNo);

        return ResponseEntity.status(HttpStatus.OK).body(updatedQuestion);
    }

    //댓글 달기 요청시
    @PostMapping("/question/comment/new")
    public ResponseEntity<Object> createComment(@Valid QuestionCommentFormDto questionCommentFormDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) { //빈칸있을 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        Question question = communityQuestionService.findQuestion(questionCommentFormDto.getProgramNo(), questionCommentFormDto.getQuestionNo());
        if (question == null) { //해당 프로그램 번호와 질문 번호에 해당하는 질문이 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no question error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        QuestionComment questionComment = QuestionComment.createComment(questionCommentFormDto, email);

        QuestionComment savedQuestionComment = communityQuestionService.saveComment(questionComment);

        return ResponseEntity.status(HttpStatus.OK).body(savedQuestionComment);
    }

    //댓글 수정 요청시
    @PostMapping("/question/comment/modify")
    public ResponseEntity<Object> modifyComment(@Valid QuestionCommentFormDto questionCommentFormDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors() || questionCommentFormDto.getCommentNo() == 0) { //빈칸있을 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("blank error");
        }
        QuestionComment questionComment = questionCommentRepository.findByProgramNoAndQuestionNoAndCommentNo(questionCommentFormDto.getProgramNo(), questionCommentFormDto.getQuestionNo(), questionCommentFormDto.getCommentNo());
        if (questionComment == null) { //해당 프로그램 번호와 질문 번호, 댓글 번호에 해당하는 질문이 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no comment error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String myEmail = ((UserDetails) principal).getUsername();
        if (!questionComment.getEmail().equals(myEmail)) { // 작성자가 아닐 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not writer error");
        }
        QuestionComment updatedQuestionComment = QuestionComment.updateComment(questionCommentFormDto, questionComment);

        QuestionComment savedQuestionComment = communityQuestionService.updateComment(updatedQuestionComment);

        return ResponseEntity.status(HttpStatus.OK).body(savedQuestionComment);
    }

    //댓글 삭제 요청시
    @PostMapping("/question/comment/delete")
    public ResponseEntity<Object> deleteComment(@ModelAttribute QuestionCommentFormDto questionCommentFormDto) {

        QuestionComment questionComment = questionCommentRepository.findByProgramNoAndQuestionNoAndCommentNo(questionCommentFormDto.getProgramNo(), questionCommentFormDto.getQuestionNo(), questionCommentFormDto.getCommentNo());
        if (questionComment == null) { //해당 프로그램 번호와 질문 번호, 댓글 번호에 해당하는 질문이 없을 때
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no comment error");
        }
        //사용자 email 얻기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String myEmail = ((UserDetails) principal).getUsername();
        if (!questionComment.getEmail().equals(myEmail)) { // 작성자가 아닐 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("not writer error");
        }
        communityQuestionService.deleteComment(questionCommentFormDto);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}
