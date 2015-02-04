(ns total-credit-hours.core
  (:require [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :as s])
  (:gen-class))


(def SQLDB {:classname "sun.jdbc.odbc.JdbcOdbcDriver"
            :subprotocol "odbc"
            ;:subname "BANNER"
            :subname "TEST"
            :user "xxx"
            :password "xxx"})

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


(def stvcoll ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY"])

(def stvcamp-coll
  {:C ["AR" "EP" "FA" "NU" "SC" "YY"]
   :L ["AR" "EP" "FA" "NU" "SC" "YY"]
   :F ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY"]
   :U ["AD" "AR" "ED" "EN" "EP" "EX" "FA" "GS" "KI" "NU" "SC" "SW" "SP" "YY"]})

(defn get-table [term-code camp-code]
  (for [camp-coll (:C stvcamp-coll)]
    (for [student-coll stvcoll]
      (let [count-val (val (first (first (j/query SQLDB [(str "select UOFREXEC.F_GET_TOTAL_CREDIT_HOURS_RL('" term-code "','" student-coll "','" camp-code "','" camp-coll "') from dual;")]))))]
        (if (nil? count-val)
          0
          count-val)))))

(get-table "201430" "C")
