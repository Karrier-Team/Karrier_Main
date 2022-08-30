package com.karrier.mentoring.service;

import com.karrier.mentoring.dto.ProgramInformationDto;
import com.karrier.mentoring.dto.ProgramViewDto;
import com.karrier.mentoring.entity.Curriculum;
import com.karrier.mentoring.entity.Mentor;
import com.karrier.mentoring.entity.ParticipationStudent;
import com.karrier.mentoring.entity.Program;
import com.karrier.mentoring.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProgramService {
    private final ProgramRepository programRepository;

    private final MentorRepository mentorRepository;

    private final MemberRepository memberRepository;

    private final CurriculumRepository curriculumRepository;

    private final ParticipationStudentRepository participationStudentRepository;

    @Transactional
    public Program createProgram(Program program){
        return programRepository.save(program);
    }

    @Transactional
    public Program updateProgram(Program program){
        return programRepository.save(program);
    }

    public Program getProgramByNo(long programNo){
        return programRepository.findByProgramNo(programNo);

    }


    public List<ProgramViewDto> getPrograms(List<String> emails, String orderType, String searchType, String searchWord)
    {
        List<Program> tempProgramList = new ArrayList<>();
        List<Program> programList = new ArrayList<>();

        if(searchType.equals("프로그램제목")){

            for(String email : emails){
                tempProgramList.addAll(programRepository.findByEmail(email));
            }

            if(searchWord == null){
                for(Program program : tempProgramList){
                    if(program.getProgramState().equals(true)){
                        programList.add(program);
                    }
                }
            }
            else{
                for(Program program : tempProgramList){
                    if((program.getTitle().contains(searchWord)) && (program.getProgramState().equals(true))){
                        programList.add(program);
                    }
                }
            }

            if(orderType.equals("최신순")){
                programList.sort(Comparator.comparing(Program::getCreateDate).reversed());
            }
            else if(orderType.equals("찜갯수")){
                programList.sort(Comparator.comparing(Program::getLikeCount).reversed());
            }
            else if(orderType.equals("이름순")){
                programList.sort(Comparator.comparing(Program::getTitle));
            }

        }
        else if(searchType.equals("멘토이름")){

            for(String email : emails){
                tempProgramList.addAll(programRepository.findByEmail(email));
            }

            if(searchWord == null){
                for(Program program : tempProgramList){
                    if(program.getProgramState().equals(true)){
                        programList.add(program);
                    }
                }
            }
            else{
                for(Program program : tempProgramList){
                    if((mentorRepository.findByEmail(program.getEmail()).getName().contains(searchWord))&& (program.getProgramState().equals(true))){
                        programList.add(program);
                    }
                }
            }

            if(orderType.equals("최신순")){
                programList.sort(Comparator.comparing(Program::getCreateDate).reversed());
            }
            else if(orderType.equals("찜갯수")){
                programList.sort(Comparator.comparing(Program::getLikeCount).reversed());
            }
            else if(orderType.equals("이름순")){
                programList.sort(Comparator.comparing(Program::getTitle));
            }
        }

        ArrayList<ProgramViewDto> programViewDtoArrayList = getProgramViewDtoList(programList);

        return programViewDtoArrayList;
    }

    public List<ProgramViewDto> getWishPrograms(List<Program> programs, String orderType, String searchType, String searchWord)
    {
        List<Program> programList = new ArrayList<>();

        if(searchType.equals("프로그램제목")){

            if(searchWord == null){
                for(Program program : programs){
                    if(program.getProgramState().equals(true)){
                        programList.add(program);
                    }
                }
            }
            else{
                for(Program program : programs){
                    if((program.getTitle().contains(searchWord)) && (program.getProgramState().equals(true))){
                        programList.add(program);
                    }
                }
            }

            if(orderType.equals("최신순")){
                programList.sort(Comparator.comparing(Program::getCreateDate).reversed());
            }
            else if(orderType.equals("제목순")){
                programList.sort(Comparator.comparing(Program::getTitle));
            }

        }
        else if(searchType.equals("멘토이름")){

            if(searchWord == null){
                for(Program program : programs){
                    if(program.getProgramState().equals(true)){
                        programList.add(program);
                    }
                }
            }
            else{
                for(Program program : programs){
                    if((mentorRepository.findByEmail(program.getEmail()).getName().contains(searchWord))&& (program.getProgramState().equals(true))){
                        programList.add(program);
                    }
                }
            }

            if(orderType.equals("최신순")){
                programList.sort(Comparator.comparing(Program::getCreateDate).reversed());
            }
            else if(orderType.equals("제목순")){
                programList.sort(Comparator.comparing(Program::getTitle));
            }
        }

        ArrayList<ProgramViewDto> programViewDtoArrayList = getProgramViewDtoList(programList);

        return programViewDtoArrayList;
    }

    public ArrayList<ProgramViewDto> getProgramViewDtoList(List<Program> programs){
        ArrayList<ProgramViewDto> programViewDtoArrayList = new ArrayList<>();

        for(Program program : programs){
            String name = mentorRepository.findByEmail(program.getEmail()).getName();
            String major = mentorRepository.findByEmail(program.getEmail()).getMajor();
            String profileImage = memberRepository.findByEmail(program.getEmail()).getProfileImage().getStoreFileName();

            programViewDtoArrayList.add(ProgramViewDto.createProgramViewDto(program, name, profileImage, major));
        }

        return programViewDtoArrayList;

    }

    public List<Program> getProgramsByEmail(String email){
        return programRepository.findByEmail(email);
    }

    public List<Program> getProgramsByEmailAndState(String email, Boolean complete){
        return programRepository.findByEmailAndProgramState(email, complete);
    }

    public List<Program> getProgramsByEmailsAndState(List<String> emails){
        return programRepository.findByProgramStateAndEmailIn(true, emails);
    }

    public List<Program> getProgramsByTitleContaining(List<String> emails, String title){
        return programRepository.findByProgramStateAndEmailInAndTitleContaining(true, emails, title);
    }

    public ProgramInformationDto getProgramInformationDto(long programNo){
        Program program = programRepository.findByProgramNo(programNo);
        Mentor mentor = mentorRepository.findByEmail(program.getEmail());
        String profileImage = memberRepository.findByEmail(program.getEmail()).getProfileImage().getStoreFileName();
        List<Curriculum> curriculumList = curriculumRepository.findByProgramNo(programNo);
        List<ParticipationStudent> participationStudentList = participationStudentRepository.findByProgramNo(programNo);

        ProgramInformationDto programInformationDto = ProgramInformationDto.createProgramInformationDto(program, mentor, profileImage, curriculumList, participationStudentList);

        return programInformationDto;
    }
}