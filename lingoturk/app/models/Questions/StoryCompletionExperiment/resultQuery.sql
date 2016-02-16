SELECT * FROM
	(SELECT workerId,round(EXTRACT(epoch FROM min(duration))) as duration,partId as list FROM
		(SELECT workerId,partId,(endT - beginT) AS duration FROM
			(SELECT workerId,partId,timestamp AS beginT FROM Workers_ParticipateIn_Parts) AS tmp1
		JOIN
			(SELECT workerId,timestamp AS endT FROM StoryCompletionResults) AS tmp2
		USING (WorkerId)) as tmp3
	GROUP BY workerId,partId) as tmp4
JOIN
	(SELECT DISTINCT * FROM
	(SELECT workerId,split_part(itemId,',',1) AS itemId, split_part(itemId,',',2) AS condition, result as answer FROM StoryCompletionResults) AS tmp5
	ORDER BY workerId) AS tmp6
USING (workerId)
WHERE condition != 'Filler'
ORDER by workerId,itemId