SELECT fileName, listNumber, ElicitingParaphrases_text, ElicitingParaphrases_fileName, ElicitingParaphrases_type, ElicitingParaphrases_tplan, ElicitingParaphrases_shortId, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM ElicitingParaphrasesResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 341
ORDER BY partId, questionId, workerId