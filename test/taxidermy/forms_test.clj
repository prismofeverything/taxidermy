(ns taxidermy.forms-test
  (:use clojure.test
        taxidermy.forms)
  (:require
        [taxidermy.validation :as validation]
        [taxidermy.fields :as fields]))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defmacro str-contains? [haystack needle]
  `(is (.contains ~haystack ~needle)))

(defmacro is-equal? [a b]
  `(is (= ~a ~b)))

(def min-length-error "Must be at least two characters")
(def max-length-error "Must not be longer than 20 characters")

(defform contact
  :fields [
            (fields/text-field :label "First Name"
                        :field-name "first_name"
                        :validators [(validation/field-validator (validation/min-length? 2) min-length-error)
                                     (validation/field-validator (validation/max-length? 20) (fn [] max-length-error))])
            (fields/text-field :label "Last Name"
                        :field-name "last_name")
            (fields/text-field :label "Email"
                        :field-name "email")
          ])

(defform all-widgets
  :fields [
            (fields/text-field :label "First Name"
                        :field-name "first_name")
            (fields/text-field :label "Last Name"
                        :field-name "last_name")
            (fields/text-field :label "Email"
                        :field-name "email")
            (fields/textarea-field :label "Description"
                        :field-name "description")
            (fields/integer-field :label "Age"
                           :field-name "age")
            (fields/select-field :label "Newsletter"
                          :field-name "newsletter"
                          :multiple true
                          :choices [["Yes", 0]
                                    ["No", 1]])
            (fields/radio-field :field-name "question1" :label "Question 1"
                         :choices [["Yes", 0]
                                    ["No", 1]])
            (fields/boolean-field :label "Mark Yes"
                            :field-name "yes"
                            :value "yes")
            (fields/boolean-field :label "Mark No"
                            :field-name "no"
                            :value "no")
          ])

(defform checkboxes
  :fields [
            (fields/boolean-field :label "Mark Yes"
                            :field-name "yes"
                            :value "yes")
            (fields/boolean-field :label "Mark No"
                            :field-name "no"
                            :value "no")
          ])

(defform select
  :fields [(fields/select-field :field-name "ghost"
                                :choices [["Face" "1"] ["Killah" 2]])])

(defform multi-select
  :fields [(fields/select-field :label "multi" :field-name "multi" :multiple true :choices [["Yes" 1] ["No" 0]])])

(deftest test-defform
  (testing "Testing defform"
    (let [test-form (contact {})]
      (is (= 3 (count (:fields test-form)))))))

(deftest test-validate
  (testing "Testing validate"
    (let [test-form (contact {:first_name "Bob"})
          errors (validation/validate test-form)]
      (is (not (validation/has-errors? errors))))))

(deftest test-invalid-select-value
  (testing "Testing invalid select value"
    (let [test-form (select {:ghost 3})
          errors (validation/validate test-form)]
      (is (validation/has-errors? errors)))))

(deftest test-errors
  (testing "Testing errors"
    ; this fails the length validation
    (let [test-form (contact {:first_name "Bobsd fadsjfosidfj dofidjsf oisdfjoisf jsdoifjdsf"})
          errors (validation/validate test-form)]
      ; check to see that we have one erro
      (is-equal? 1 (count (:first_name errors)))
      ; check to make sure this helper func returns true
      (is (validation/has-errors? errors)))))

(deftest test-minlength
  (testing "Testing min-length"
    (let [test-form (contact {:first_name "B"})
          errors (validation/validate test-form)
          firstname-errors (:first_name errors)]
      (is (in? firstname-errors min-length-error)))))

(deftest test-maxlength-with-func
  (testing "Testing max-length"
    (let [test-form (contact {:first_name "Bob Boboboboboboboboboboboboobboob"})
          errors (validation/validate test-form)
          firstname-errors (:first_name errors)]
      (is (in? firstname-errors max-length-error)))))

(deftest test-checkboxes
  (testing "Testing checkboxes"
    (let [test-form (checkboxes {:yes "yes"})
          yes-box (:yes (:fields test-form))
          no-box (:no (:fields test-form))
          processed-vals (processed-values test-form)]
      ; check rendered values
      (str-contains? (str yes-box) "checked=\"checked\"")
      (is (not (.contains (str no-box) "checked=\"checked\"")))

      ; check process value
      (is (:yes processed-vals)))))

(deftest test-multi-select
  (testing "Testing multi-select"
    (let [test-form (multi-select {})
          select-field (:multi (:fields test-form))]
      (str-contains? (str select-field) "multiple=\"multiple\""))))

(deftest test-select-choices
  (testing "Testing scalar Select choices"
    (let [field-name :rating
          id "rating"
          field-data 5
          choices (range 1 11)
          select (fields/select-field :field-name field-name
                                      :process-func fields/integer-processor
                                      :choices choices
                                      :data field-data)
          first-option (first (.options select))]
      (is (= (:value first-option) 1))
      (is (= (:text first-option) 1))
      (is (:selected (first (filter #(= 5 (:value %)) (.options select)))))))
  (testing "Testing tuple Select choices (with default)"
    (let [field-name :rating
          id "rating"
          field-data 5
          range-vals (range 1 11)
          default-choice ["default-text" "default-value"]
          choices (map (juxt identity identity) range-vals)
          select (fields/select-field :field-name field-name
                                      :process-func fields/integer-processor
                                      :default-choice default-choice
                                      :choices choices
                                      :data field-data)
          default-option (first (.options select))
          first-option (second (.options select))]
      (is (= (:text default-option (first default-choice))))
      (is (= (:value default-option (second default-choice))))
      (is (= (:value first-option) 1))
      (is (= (:text first-option) 1))
      (is (:selected (first (filter #(= 5 (:value %)) (.options select))))))))
