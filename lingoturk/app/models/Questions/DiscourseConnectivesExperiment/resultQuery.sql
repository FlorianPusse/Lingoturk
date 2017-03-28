SELECT fileName, listNumber, DiscourseConnectives_sentence1, DiscourseConnectives_sentence2, DiscourseConnectives_innerID, DiscourseConnectives_sentenceType, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM DiscourseConnectivesResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 101
ORDER BY partId, questionId, workerId