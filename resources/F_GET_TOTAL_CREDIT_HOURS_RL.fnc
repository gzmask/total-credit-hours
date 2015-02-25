CREATE OR REPLACE FUNCTION F_GET_TOTAL_CREDIT_HOURS_RL
(term_code_current IN varchar3,
 student_coll_code IN char,
 course_camp_code IN char,
 course_coll_code IN char)
/**
 * Calculate total credit hours of students from student_coll_code
 * who have took classes with course_coll_code on campus with course_camp_code
 * term_code_current '201430'
 * student_coll_code 'BU'
 * course_camp_code 'U'
 * course_coll_code 'AR'
 * @created-by: LEI203
 * @created-on: 4/Feb/2015
 * @tags: credit-hours, students, schedule
 */
-- term_code_current '201430'
-- student_coll_code 'BU'
-- course_camp_code 'U'
-- course_coll_code 'AR'
-- SVN $Id:
-- author: Ray Lei
-- created: 4/Feb/2015
-- purpose: Calculate total credit hours of students from student_coll_code
--          who have took classes with course_coll_code on campus with course_camp_code

RETURN number
IS
result number:= 0;
BEGIN

WITH  Q_cat_schd AS
(SELECT
   UOFREXEC.Y_CAT_SCHD.TERM_CODE,
   UOFREXEC.Y_CAT_SCHD.CRN,
   UOFREXEC.Y_CAT_SCHD.CREDIT_HRS,
   UOFREXEC.Y_CAT_SCHD.CAMP_CODE,
 DECODE(substr(Y_CAT_SCHD.SEQ_NUMB,0,1), 'L', 'L',
                                         'S', 'S',
                                         'C', 'C',
                                         'F', 'F',
                                         'U') AS CAMPUS,
 DECODE(Y_CAT_SCHD.COLL_CODE, 'BU', 'AD',
                              'CE', 'EX',
                              'PA', 'KI',
                              'ES', 'EN',
                              Y_CAT_SCHD.COLL_CODE) AS COLL
 FROM UOFREXEC.Y_CAT_SCHD),

Course_Camp_Coll_T AS
(SELECT Q_cat_schd.TERM_CODE,
        Q_cat_schd.CRN,
        Q_cat_schd.CREDIT_HRS,
        CONCAT(Q_cat_schd.CAMPUS,
               Q_cat_schd.COLL) AS Camp_and_Coll,
        Q_cat_schd.CAMP_CODE
 FROM Q_cat_schd
 WHERE ((Q_cat_schd.TERM_CODE)=term_code_current) AND (Q_cat_schd.CAMP_CODE='1')),

Student_Camp_Coll_T AS
(SELECT UOFREXEC.Y_STUDENT_CAMP_COLL.TERM_CODE,
        UOFREXEC.Y_STUDENT_CAMP_COLL.CRN,
        UOFREXEC.Y_STUDENT_CAMP_COLL.CREDIT_HOURS,
        UOFREXEC.Y_STUDENT_CAMP_COLL.CAMP_CODE,
        DECODE(UOFREXEC.Y_STUDENT_CAMP_COLL.COLL_CODE, 
               'BU', 'AD',
               'CE', 'EX',
               'PA', 'KI',
               'ES', 'EN',
               UOFREXEC.Y_STUDENT_CAMP_COLL.COLL_CODE) AS COLL_CODE
 FROM UOFREXEC.Y_STUDENT_CAMP_COLL
 WHERE (UOFREXEC.Y_STUDENT_CAMP_COLL.TERM_CODE)=term_code_current)
SELECT SUM(Student_Camp_Coll_T.CREDIT_HOURS) INTO result
FROM Course_Camp_Coll_T, Student_Camp_Coll_T
WHERE Course_Camp_Coll_T.TERM_CODE=term_code_current AND Student_Camp_Coll_T.TERM_CODE=term_code_current
AND Course_Camp_Coll_T.CRN=Student_Camp_Coll_T.CRN
-- Faculty of Bussiness Admin student who took Arts class in Campion
AND Student_Camp_Coll_T.COLL_CODE=student_coll_code
AND Course_Camp_Coll_T.CAMP_AND_COLL=CONCAT(course_camp_code,course_coll_code);
return result;
END F_GET_TOTAL_CREDIT_HOURS_RL;
/
