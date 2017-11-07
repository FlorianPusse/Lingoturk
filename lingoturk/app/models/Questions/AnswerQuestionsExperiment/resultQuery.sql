SELECT fileName, listNumber, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer::TEXT, (data->>'story') as story, (data->>'questions') as questions FROM (
	(SELECT * FROM Results WHERE experimentType='AnswerQuestionsExperiment') as tmp1
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 825
ORDER BY partId, questionId, workerId