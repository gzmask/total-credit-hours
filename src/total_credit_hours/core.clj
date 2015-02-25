(ns total-credit-hours.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :as s])
  (:gen-class))

(def SQLDB (atom
            {:classname "oracle.jdbc.OracleDriver"
             :subprotocol "oracle:oci"
             ;:subname "@TEST"
             ;:user "lei203"
             ;:password "hatehatehatehatehate"
             }))

(def stvcoll-all {
  :JS "~Johnson-Shoyama Grad School"
  :NU "Nursing"
  :BU "Business Administration"
  :PP "Grad School of Public Policy"
  :SC "Science"
  :SW "Social Work"
  :SP "Special"
  :YY "Interim"
  :AS "~Arts & Sciences"
  :CM "~Conservatory of Music"
  :EL "~English as a Second Language"
  :RO "Registrar's Office(Rooms Only)"
  :NC "Non-Credit Programs"
  :CE "Continuing Education"
  :KI "Kinesiology & Health Studies"
  :00 "No College Designated"
  :AD "~Administration"
  :AR "Arts"
  :ED "Education"
  :EN "~Engineering"
  :EP "~University Entrance Program"
  :EX "~University Extension"
  :FA "Fine Arts"
  :99 "Unused college in stat calc"
  :GS "Graduate Studies & Research"
  :PA "~Physical Activity Studies"
  :SA "Student Affairs"
  :ZZ "UofR Staff (Admin. Services)"
  :ES "Engineering & Applied Science"})

(def stvcamp-all {
  :S "~ SIFC"
  :1 "On"
  :C "Campion"
  :L "Luther"
  :U "U of R"
  :2 "Off"
  :F "FN Univ"})


(def stvcoll ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY" "00"])
(def stvcoll-names (map #((keyword %) stvcoll-all) stvcoll))

(def stvcamp-coll
  {:C ["AR" "EP" "FA" "NU" "SC" "YY" "00"]
   :L ["AR" "EP" "FA" "NU" "SC" "YY" "00"]
   :S ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY" "00"]
   :U ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY" "00"]})

(defn get-total-credit-hours-rl
  [term-code-current student-coll-code course-camp-code course-coll-code]
  (str
  "WITH  Q_cat_schd AS
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
 WHERE ((Q_cat_schd.TERM_CODE)='" term-code-current "') AND (Q_cat_schd.CAMP_CODE='1')),

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
 WHERE (UOFREXEC.Y_STUDENT_CAMP_COLL.TERM_CODE)='" term-code-current "')
SELECT SUM(Student_Camp_Coll_T.CREDIT_HOURS)
FROM Course_Camp_Coll_T, Student_Camp_Coll_T
WHERE Course_Camp_Coll_T.TERM_CODE='" term-code-current "' AND Student_Camp_Coll_T.TERM_CODE='" term-code-current "'
AND Course_Camp_Coll_T.CRN=Student_Camp_Coll_T.CRN
-- Faculty of Bussiness Admin student who took Arts class in Campion
AND Student_Camp_Coll_T.COLL_CODE='" student-coll-code "'
AND Course_Camp_Coll_T.CAMP_AND_COLL=CONCAT('" course-camp-code "','" course-coll-code "')"))

(defn row-total [h s]
  {:pre [(seq? s)
         (> (count s) 0)]
   :post [(= (count %) (+ 2 (count s)))]}
  (let [total (reduce + s)]
    (into [((keyword h) stvcoll-all)] (conj (vec s) total))))

(defn column-total [s]
  {:pre [(seq? s)
         (vector? (first s))
         (string? (first (first s)))
         (number? (second (first s)))]
   :post [(= (count %) (+ 1 (count s)))]}
  (conj (vec s) (into ["totals:"] (vec  (apply map + (map rest s))))))

;(str "select UOFREXEC.F_GET_TOTAL_CREDIT_HOURS_RL('" term-code "','" student-coll "','" camp-code "','" camp-coll "') from dual;")

(defn get-table [term-code camp-code]
  (column-total (for [camp-coll ((keyword camp-code) stvcamp-coll)]
    (row-total camp-coll (for [student-coll stvcoll]
      (let [count-val (val (first (first (j/query @SQLDB [(get-total-credit-hours-rl term-code student-coll camp-code camp-coll)]))))]
        (if (nil? count-val)
          0
          count-val)))))))

;(get-table "201410" "C")
;(get-total-credit-hours-rl "2001410" "AR" "AR" "C")

(defn get-csv-camp
  [camp-code camp-tb]
  (str
     \newline
     ;header
     (str (camp-code stvcamp-all) ": " \newline)
     ;students
     (str ", " (apply str (interleave stvcoll-names (repeat ", "))) " total, " \newline)
     ;numbers
     (apply str (for [row (drop-last camp-tb)]
                  (str (apply str (interleave row (repeat ", "))) \newline)))
     ;totals
     (str (apply str (interleave (last camp-tb) (repeat ", "))) \newline)
))

(defn report-csv
  [term-code]
  (let [c (get-table term-code "C")
        l (get-table term-code "L")
        s (get-table term-code "S")
        u (get-table term-code "U")]
    (str
      (get-csv-camp :C c)
      (get-csv-camp :L l)
      (get-csv-camp :S s)
      (get-csv-camp :U u))))

(def cli-options
  [["-h" "--help"]
   ["-t" "--termcode termcode" "total -t [term code such as 200930]."
    :id :term
    :validate [#(re-find #"\d{6}" %) "Must be a 9 digit number."]]
   ["-u" "--username username" "total -u [username such as lll200]."
    :id :username]
   ["-p" "--password password" "total -p [password]."
    :id :password]
   ["-d" "--db dbname" "total -d [database name]. TEST, QPRD, DEVL etc."
    :id :db
    :default "TEST"]
   ["-c" "--csv csv-file" "total -c [csv-filename]."
    :id :csv
    :default "result.csv"]
   ])

(def help-txt "to get the hours total, simply run: \"total -t [term code] -c [csv file] -d [dbname] -u [username] -p [password]\" ")

(defn exit [status & msg]
  (when msg (println msg))
  (System/exit status))

(defn -main [& args]
  (let [{opts :options args :arguments summary :summary errs :errors}
        (parse-opts args cli-options)]
    (when (not (empty? errs))
      (doseq [err errs]
        (println err))
      (exit 1))
    (when (:help opts)
      (println help-txt)
      (exit 0 summary))
    (when (and (:username opts)
               (:password opts))
      (swap! SQLDB #(assoc % :user (:username opts)
                             :password (:password opts)
                             :subname (str "@" (:db opts)))))
    (when (and (:term opts)
               (:csv opts)
               (nil? (:help opts)))
      (spit (:csv opts) (report-csv (:term opts))))))
