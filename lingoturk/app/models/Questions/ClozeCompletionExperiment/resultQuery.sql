SELECT fileName, listNumber, ClozeCompletion_arg1, ClozeCompletion_arg2, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer FROM (
	SELECT * FROM ClozeCompletionResults
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 41
ORDER BY partId, questionId, workerId