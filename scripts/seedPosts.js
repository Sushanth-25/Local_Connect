const admin = require('firebase-admin');
const path = require('path');

const KEY_PATH = path.join(__dirname, 'serviceAccountKey.json');

try {
  const serviceAccount = require(KEY_PATH);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('Firebase Admin initialized successfully');
} catch (err) {
  console.error('Failed to load serviceAccountKey.json from scripts/');
  console.error('Please download it from Firebase Console → Project Settings → Service Accounts');
  process.exit(1);
}

const db = admin.firestore();

// Categories from PostCategory.kt
const CATEGORIES = [
  'GENERAL', 'HEALTH', 'ROADS', 'INFRASTRUCTURE',
  'LOST_AND_FOUND', 'EVENTS', 'WASTE', 'WATER_SUPPLY',
  'SPORTS', 'CULTURE', 'EMERGENCY'
];

const STATUSES = ['Open', 'In Progress', 'Resolved', 'Closed'];
const POST_TYPES = ['ISSUE', 'POST', 'EVENT'];

// Sample data for realistic posts
const ISSUE_TEMPLATES = {
  ROADS: [
    { title: 'Pothole on Main Street', description: 'Large pothole causing traffic issues and potential vehicle damage.' },
    { title: 'Broken Street Light', description: 'Street light has been out for several days, creating safety concerns at night.' },
    { title: 'Traffic Signal Malfunction', description: 'Traffic light stuck on red, causing major delays.' },
    { title: 'Road Crack Needs Repair', description: 'Significant crack developing across the road surface.' },
    { title: 'Missing Road Sign', description: 'Stop sign has been knocked down and needs replacement.' },
  ],
  WASTE: [
    { title: 'Overflowing Garbage Bin', description: 'Public bin is overflowing and attracting pests.' },
    { title: 'Illegal Dumping Site', description: 'Large amount of waste illegally dumped in public area.' },
    { title: 'Missed Garbage Collection', description: 'Scheduled pickup was missed this week.' },
    { title: 'Broken Recycling Container', description: 'Recycling bin damaged and needs replacement.' },
    { title: 'Littering Problem', description: 'Persistent littering issue in park area.' },
  ],
  WATER_SUPPLY: [
    { title: 'Water Leak on Street', description: 'Visible water leak from underground pipe causing puddles.' },
    { title: 'Low Water Pressure', description: 'Experiencing very low water pressure in the neighborhood.' },
    { title: 'Burst Water Main', description: 'Major water main break flooding the street.' },
    { title: 'Contaminated Water Report', description: 'Water appears discolored and has unusual odor.' },
    { title: 'Fire Hydrant Leaking', description: 'Fire hydrant continuously dripping water.' },
  ],
  INFRASTRUCTURE: [
    { title: 'Damaged Sidewalk', description: 'Cracked and uneven sidewalk poses tripping hazard.' },
    { title: 'Broken Park Bench', description: 'Bench in community park is broken and needs repair.' },
    { title: 'Graffiti on Public Building', description: 'Vandalism on community center wall.' },
    { title: 'Damaged Fence', description: 'Fence around playground is broken and unsafe.' },
    { title: 'Bridge Railing Damage', description: 'Section of bridge railing is loose and dangerous.' },
  ],
  HEALTH: [
    { title: 'Mosquito Breeding Site', description: 'Stagnant water creating mosquito breeding ground.' },
    { title: 'Dead Animal on Road', description: 'Animal carcass needs to be removed from roadway.' },
    { title: 'Air Quality Concern', description: 'Unusual smoke or fumes in the area.' },
    { title: 'Overgrown Vegetation', description: 'Overgrown weeds and bushes blocking pathways.' },
    { title: 'Public Restroom Unsanitary', description: 'Park restroom in poor condition and needs cleaning.' },
  ],
  LOST_AND_FOUND: [
    { title: 'Found: Black Wallet', description: 'Found wallet near bus stop, contains ID and cards.' },
    { title: 'Lost: Golden Retriever', description: 'Missing dog, answers to Max, very friendly.' },
    { title: 'Found: Keys on Keychain', description: 'Set of keys found in parking lot.' },
    { title: 'Lost: Backpack', description: 'Blue backpack lost near school, contains laptop.' },
    { title: 'Found: Bicycle', description: 'Abandoned bicycle found in park.' },
  ],
  EVENTS: [
    { title: 'Community Clean-up Day', description: 'Join us for neighborhood clean-up this Saturday morning.' },
    { title: 'Local Farmers Market', description: 'Weekly farmers market open every Sunday.' },
    { title: 'Town Hall Meeting', description: 'Public meeting to discuss community development plans.' },
    { title: 'Charity Run Event', description: '5K run to raise funds for local charity.' },
    { title: 'Street Festival', description: 'Annual street festival with food, music, and activities.' },
  ],
  SPORTS: [
    { title: 'Soccer Field Maintenance', description: 'Community soccer field needs grass repair and line marking.' },
    { title: 'Basketball Court Resurfacing', description: 'Court surface is worn and slippery when wet.' },
    { title: 'Tennis Net Broken', description: 'Tennis court net is torn and needs replacement.' },
    { title: 'Fitness Equipment Broken', description: 'Outdoor gym equipment not functioning properly.' },
    { title: 'Running Track Damage', description: 'Track surface has cracks and uneven sections.' },
  ],
  CULTURE: [
    { title: 'Library Book Donation Drive', description: 'Collecting book donations for community library.' },
    { title: 'Art Exhibition Opening', description: 'Local artists showcasing work at community center.' },
    { title: 'Cultural Festival Planning', description: 'Organizing multicultural celebration event.' },
    { title: 'Historic Site Preservation', description: 'Fundraising for local historic building restoration.' },
    { title: 'Community Theater Production', description: 'Auditions for upcoming community play.' },
  ],
  EMERGENCY: [
    { title: 'Downed Power Line', description: 'Power line down across road - STAY AWAY, very dangerous!' },
    { title: 'Tree Fallen Blocking Road', description: 'Large tree blocking entire roadway after storm.' },
    { title: 'Gas Leak Smell', description: 'Strong gas odor in residential area - possible leak.' },
    { title: 'Flooding on Street', description: 'Severe flooding making road impassable.' },
    { title: 'Hazardous Material Spill', description: 'Unknown chemical spill on roadway.' },
  ],
  GENERAL: [
    { title: 'Noise Complaint', description: 'Excessive noise from construction site outside permitted hours.' },
    { title: 'Stray Animals', description: 'Several stray dogs roaming the neighborhood.' },
    { title: 'Parking Violation', description: 'Vehicle blocking fire hydrant access.' },
    { title: 'Community Suggestion', description: 'Proposal for new crosswalk near school.' },
    { title: 'Public Notice', description: 'Information about upcoming road closure for maintenance.' },
  ]
};

// Generate realistic location names
const LOCATION_PREFIXES = [
  'Main', 'Oak', 'Maple', 'Park', 'Lake', 'River', 'Hill', 'Forest',
  'Cedar', 'Pine', 'Elm', 'Washington', 'Lincoln', 'Jefferson', 'Madison',
  'First', 'Second', 'Third', 'Center', 'North', 'South', 'East', 'West'
];

const LOCATION_TYPES = [
  'Street', 'Avenue', 'Road', 'Boulevard', 'Drive', 'Lane', 'Way', 'Circle',
  'Court', 'Place', 'Parkway', 'Highway'
];

const LANDMARKS = [
  'Community Center', 'Park', 'Library', 'School', 'Market', 'Plaza',
  'Shopping Center', 'Recreation Center', 'Sports Complex', 'Town Hall'
];

function generateLocationName() {
  const rand = Math.random();
  if (rand < 0.7) {
    // Street address
    const prefix = LOCATION_PREFIXES[Math.floor(Math.random() * LOCATION_PREFIXES.length)];
    const type = LOCATION_TYPES[Math.floor(Math.random() * LOCATION_TYPES.length)];
    return `${prefix} ${type}`;
  } else {
    // Landmark
    const prefix = LOCATION_PREFIXES[Math.floor(Math.random() * LOCATION_PREFIXES.length)];
    const landmark = LANDMARKS[Math.floor(Math.random() * LANDMARKS.length)];
    return `${prefix} ${landmark}`;
  }
}

// Generate random coordinates within a realistic area
// Default center: roughly San Francisco area, but you can adjust
const CENTER_LAT = 37.7749;
const CENTER_LNG = -122.4194;
const RADIUS_KM = 10; // 10km radius

function generateRandomLocation() {
  // Generate random point within radius
  const radiusInDegrees = RADIUS_KM / 111.32; // Convert km to degrees (rough approximation)

  const u = Math.random();
  const v = Math.random();

  const w = radiusInDegrees * Math.sqrt(u);
  const t = 2 * Math.PI * v;

  const deltaLat = w * Math.cos(t);
  const deltaLng = w * Math.sin(t) / Math.cos(CENTER_LAT * Math.PI / 180);

  return {
    latitude: CENTER_LAT + deltaLat,
    longitude: CENTER_LNG + deltaLng,
    locationName: generateLocationName()
  };
}

function getRandomElement(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function generatePost(index) {
  const category = getRandomElement(CATEGORIES);
  const templates = ISSUE_TEMPLATES[category] || ISSUE_TEMPLATES.GENERAL;
  const template = getRandomElement(templates);

  const type = category === 'EVENTS' ? 'EVENT' :
               category === 'LOST_AND_FOUND' ? 'POST' :
               'ISSUE';

  const status = type === 'ISSUE' ? getRandomElement(STATUSES) : null;

  const location = generateRandomLocation();

  // Generate timestamp within last 30 days
  const now = Date.now();
  const thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000);
  const timestamp = Math.floor(Math.random() * (now - thirtyDaysAgo) + thirtyDaysAgo);

  // Random engagement metrics
  const likes = Math.floor(Math.random() * 50);
  const comments = Math.floor(Math.random() * 20);
  const upvotes = Math.floor(Math.random() * 100);
  const views = Math.floor(Math.random() * 500) + upvotes;

  // Priority for issues
  const priority = type === 'ISSUE' ? Math.floor(Math.random() * 3) + 1 : null;

  return {
    postId: `seed_${Date.now()}_${index}`,
    userId: 'system_seed',
    title: template.title,
    description: template.description,
    caption: template.description.substring(0, 100),
    category: category,
    status: status,
    type: type,
    latitude: location.latitude,
    longitude: location.longitude,
    locationName: location.locationName,
    timestamp: timestamp,
    updatedAt: timestamp,
    likes: likes,
    comments: comments,
    upvotes: upvotes,
    views: views,
    priority: priority,
    hasImage: false,
    mediaUrls: [],
    thumbnailUrls: [],
    tags: [],
    isLocalOnly: true
  };
}

async function seedPosts(count = 100) {
  console.log(`\n🌱 Starting to seed ${count} posts...\n`);

  const batch = db.batch();
  const posts = [];

  for (let i = 0; i < count; i++) {
    const post = generatePost(i);
    posts.push(post);

    const docRef = db.collection('posts').doc(post.postId);
    batch.set(docRef, post);

    if ((i + 1) % 10 === 0) {
      console.log(`Generated ${i + 1}/${count} posts...`);
    }
  }

  console.log('\n📝 Writing to Firestore...');
  await batch.commit();

  console.log('\n✅ Successfully seeded posts!\n');

  // Print summary
  const categoryCounts = {};
  const typeCounts = {};

  posts.forEach(post => {
    categoryCounts[post.category] = (categoryCounts[post.category] || 0) + 1;
    typeCounts[post.type] = (typeCounts[post.type] || 0) + 1;
  });

  console.log('📊 Summary:');
  console.log('\nBy Type:');
  Object.entries(typeCounts).forEach(([type, count]) => {
    console.log(`  ${type}: ${count}`);
  });

  console.log('\nBy Category:');
  Object.entries(categoryCounts).forEach(([category, count]) => {
    console.log(`  ${category}: ${count}`);
  });

  console.log(`\n📍 Location: Random points within ${RADIUS_KM}km of (${CENTER_LAT}, ${CENTER_LNG})`);
  console.log('   (You can adjust CENTER_LAT, CENTER_LNG, and RADIUS_KM in the script)\n');
}

// Parse command line arguments
const args = process.argv.slice(2);
const count = args[0] ? parseInt(args[0]) : 100;

if (isNaN(count) || count <= 0) {
  console.error('Usage: node scripts/seedPosts.js [count]');
  console.error('Example: node scripts/seedPosts.js 100');
  process.exit(1);
}

seedPosts(count)
  .then(() => {
    console.log('Done!');
    process.exit(0);
  })
  .catch(error => {
    console.error('Error seeding posts:', error);
    process.exit(1);
  });
