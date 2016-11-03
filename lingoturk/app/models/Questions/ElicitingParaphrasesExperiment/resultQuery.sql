SELECT ElicitingParaphrasesResults.*, ElicitingParaphrases_type, ElicitingParaphrases_tplan, ElicitingParaphrases_shortId
FROM
  ElicitingParaphrasesResults
JOIN
  Questions
USING (QuestionId);