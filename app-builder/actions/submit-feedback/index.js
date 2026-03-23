const fetch = require('node-fetch');

/**
 * App Builder Action to submit human feedback to AEM.
 * This makes the HITL loop cloud-native.
 */
async function main(params) {
  const { path, source, correction, aemHost, accessToken } = params;

  if (!path || !source || !correction || !aemHost) {
    return {
      statusCode: 400,
      body: { error: 'Missing required parameters' }
    };
  }

  const url = `${aemHost}/bin/translation-gemma/feedback`;
  
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        path,
        source,
        correction
      })
    });

    if (!response.ok) {
      throw new Error(`AEM responded with ${response.status}`);
    }

    return {
      statusCode: 200,
      body: { message: 'Feedback submitted successfully via App Builder' }
    };

  } catch (error) {
    return {
      statusCode: 500,
      body: { error: error.message }
    };
  }
}

exports.main = main;
