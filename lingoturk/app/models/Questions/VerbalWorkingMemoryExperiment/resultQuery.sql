SELECT fileName, listNumber, assignmentId, hitId, workerId, origin, timestamp, partId, questionId, answer, (data->'sentence') as sentence FROM (
	(SELECT * FROM Results WHERE experimentType='VerbalWorkingMemoryExperiment') as tmp1
	LEFT OUTER JOIN Questions USING (QuestionId)
	LEFT OUTER JOIN Groups USING (PartId)
) as tmp
WHERE LingoExpModelId = 161
ORDER BY partId, questionId, workerId