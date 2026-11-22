const { Core } = require('@adobe/aio-sdk');

function json(statusCode, body) {
  return { statusCode, headers: { 'Content-Type': 'application/json' }, body };
}

async function main(params) {
  const logger = Core.Logger('submit-feedback', { level: params.LOG_LEVEL || 'info' });

  const { sourceString, originalTranslation, humanCorrection, sourceLang, targetLang } = params;
  const host = (params.aem_host || '').replace(/\/$/, '');

  if (!sourceString || !humanCorrection || !sourceLang || !targetLang) {
    return json(400, { error: 'sourceString, humanCorrection, sourceLang and targetLang are required' });
  }
  if (!host) {
    return json(500, { error: 'aem_host is not configured' });
  }

  const auth = params.aem_token
    ? `Bearer ${params.aem_token}`
    : (params.__ow_headers && params.__ow_headers.authorization) || '';
  if (!auth) {
    return json(401, { error: 'No authorization token available' });
  }

  try {
    const response = await fetch(`${host}/bin/translation-gemma/feedback`, {
      method: 'POST',
      headers: { Authorization: auth, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sourceString,
        originalTranslation: originalTranslation || '',
        humanCorrection,
        sourceLang,
        targetLang
      })
    });

    if (!response.ok) {
      const text = await response.text();
      logger.error(`AEM feedback failed (${response.status}): ${text}`);
      return json(502, { error: 'AEM feedback request failed', status: response.status });
    }

    return json(200, { message: 'Feedback recorded' });
  } catch (error) {
    logger.error(`Request to AEM failed: ${error.message}`);
    return json(502, { error: 'Request to AEM failed' });
  }
}

exports.main = main;
