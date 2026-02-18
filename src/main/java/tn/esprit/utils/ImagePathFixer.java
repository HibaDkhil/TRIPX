package tn.esprit.utils;

import tn.esprit.entities.Accommodation;
import tn.esprit.services.AccommodationService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class to fix and normalize image paths in the database
 * Run this once to standardize all image URLs to work with the improved image loading logic
 *
 * Usage: Right-click and run as Java Application in your IDE
 */
public class ImagePathFixer {
    private static final Logger LOGGER = Logger.getLogger(ImagePathFixer.class.getName());
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String UPLOADS_DIR = PROJECT_ROOT + File.separator + "uploads";

    public static void main(String[] args) {
        LOGGER.info("========================================");
        LOGGER.info("Image Path Fixer - Starting");
        LOGGER.info("Project Root: " + PROJECT_ROOT);
        LOGGER.info("Uploads Dir: " + UPLOADS_DIR);
        LOGGER.info("========================================");

        // Check if uploads directory exists
        File uploadsFolder = new File(UPLOADS_DIR);
        if (!uploadsFolder.exists()) {
            LOGGER.severe("✗ Uploads directory does not exist: " + UPLOADS_DIR);
            LOGGER.severe("Please create the uploads directory first!");
            return;
        }

        AccommodationService accommodationService = new AccommodationService();
        List<Accommodation> accommodations = null;

        try {
            accommodations = accommodationService.getAllAccommodations();
            LOGGER.info("Found " + accommodations.size() + " accommodations to process");
        } catch (Exception e) {
            LOGGER.severe("✗ Error fetching accommodations: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (accommodations == null || accommodations.isEmpty()) {
            LOGGER.warning("No accommodations found in database");
            return;
        }

        int fixed = 0;
        int notFound = 0;
        int alreadyCorrect = 0;
        int skipped = 0;

        for (Accommodation acc : accommodations) {
            String originalPath = acc.getImagePath();

            // Skip if no image path
            if (originalPath == null || originalPath.trim().isEmpty()) {
                LOGGER.info("⭐ Skipped (no path): " + acc.getName());
                skipped++;
                continue;
            }

            LOGGER.info("\n--- Processing: " + acc.getName() + " ---");
            LOGGER.info("Current path: " + originalPath);

            String fixedPath = fixAndNormalizeImagePath(originalPath);

            if (fixedPath == null) {
                LOGGER.warning("✗ Could not find image for: " + acc.getName());
                LOGGER.warning("   Original path: " + originalPath);
                notFound++;
            } else if (fixedPath.equals(originalPath)) {
                LOGGER.info("✓ Already correct: " + acc.getName());
                alreadyCorrect++;
            } else {
                LOGGER.info("🔧 Fixing path for: " + acc.getName());
                LOGGER.info("   From: " + originalPath);
                LOGGER.info("   To:   " + fixedPath);

                try {
                    acc.setImagePath(fixedPath);
                    accommodationService.updateAccommodation(acc);
                    fixed++;
                    LOGGER.info("   ✅ Updated successfully");
                } catch (Exception e) {
                    LOGGER.severe("   ✗ Failed to update: " + e.getMessage());
                }
            }
        }

        LOGGER.info("\n========================================");
        LOGGER.info("Image Path Fixer - Complete");
        LOGGER.info("========================================");
        LOGGER.info("Total processed: " + accommodations.size());
        LOGGER.info("✅ Fixed: " + fixed);
        LOGGER.info("✓ Already correct: " + alreadyCorrect);
        LOGGER.info("✗ Not found: " + notFound);
        LOGGER.info("⭐ Skipped (no path): " + skipped);
        LOGGER.info("========================================");
    }

    /**
     * Find and normalize an image path
     * Returns a normalized path relative to project root, or null if not found
     */
    private static String fixAndNormalizeImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanPath = imagePath.trim();

            // Strategy 1: Check if file exists at this exact path from project root
            Path testPath = Paths.get(PROJECT_ROOT, cleanPath);
            if (Files.exists(testPath) && Files.isRegularFile(testPath)) {
                LOGGER.info("   ✓ Path is already correct");
                return normalizePathForStorage(cleanPath);
            }

            // Strategy 2: Try without leading slash
            if (cleanPath.startsWith("/") || cleanPath.startsWith("\\")) {
                String withoutSlash = cleanPath.substring(1);
                testPath = Paths.get(PROJECT_ROOT, withoutSlash);
                if (Files.exists(testPath) && Files.isRegularFile(testPath)) {
                    LOGGER.info("   ✓ Fixed by removing leading slash");
                    return normalizePathForStorage(withoutSlash);
                }
            }

            // Strategy 3: If it's an absolute path, try to make it relative
            File absoluteFile = new File(cleanPath);
            if (absoluteFile.isAbsolute() && absoluteFile.exists()) {
                Path absolutePath = absoluteFile.toPath();
                Path projectPath = Paths.get(PROJECT_ROOT);
                if (absolutePath.startsWith(projectPath)) {
                    Path relativePath = projectPath.relativize(absolutePath);
                    String relativeStr = normalizePathForStorage(relativePath.toString());
                    LOGGER.info("   ✓ Converted absolute to relative path");
                    return relativeStr;
                }
            }

            // Strategy 4: Search by filename in uploads directory
            String filename = Paths.get(cleanPath).getFileName().toString();
            LOGGER.info("   Searching for filename: " + filename);

            Path uploadsPath = Paths.get(UPLOADS_DIR);
            if (Files.exists(uploadsPath)) {
                Path foundPath = findFileInDirectory(uploadsPath, filename);
                if (foundPath != null) {
                    // Make it relative to project root
                    Path projectPath = Paths.get(PROJECT_ROOT);
                    Path relativePath = projectPath.relativize(foundPath);
                    String normalizedPath = normalizePathForStorage(relativePath.toString());
                    LOGGER.info("   ✓ Found by searching: " + normalizedPath);
                    return normalizedPath;
                }
            }

            // Strategy 5: Try common path transformations
            String[] transformations = {
                    // Try adding uploads prefix
                    "uploads/" + cleanPath.replaceFirst("^/+", ""),

                    // Try uploads/images prefix
                    "uploads/images/" + filename,

                    // Extract type from path and reconstruct
                    reconstructPathByType(cleanPath, filename),

                    // Try type-specific directories
                    "uploads/images/hotels/" + filename,
                    "uploads/images/villas/" + filename,
                    "uploads/images/apartments/" + filename,
                    "uploads/images/airbnbs/" + filename
            };

            for (String transformation : transformations) {
                if (transformation == null) continue;

                testPath = Paths.get(PROJECT_ROOT, transformation);
                if (Files.exists(testPath) && Files.isRegularFile(testPath)) {
                    LOGGER.info("   ✓ Fixed using transformation: " + transformation);
                    return normalizePathForStorage(transformation);
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Error processing path: " + e.getMessage());
        }

        return null;
    }

    /**
     * Normalize a path for storage - use forward slashes, no leading slash
     */
    private static String normalizePathForStorage(String path) {
        if (path == null) return null;

        // Replace backslashes with forward slashes
        String normalized = path.replace("\\", "/");

        // Remove leading slash if present
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    /**
     * Try to reconstruct path based on type detection
     */
    private static String reconstructPathByType(String originalPath, String filename) {
        String type = detectTypeFromPath(originalPath);
        if (type != null) {
            return "uploads/images/" + type + "/" + filename;
        }
        return null;
    }

    /**
     * Detect accommodation type from path
     */
    private static String detectTypeFromPath(String path) {
        if (path == null) return null;

        String lower = path.toLowerCase();

        // Check for type in path
        if (lower.contains("/hotels/") || lower.contains("\\hotels\\")) return "hotels";
        if (lower.contains("/villas/") || lower.contains("\\villas\\")) return "villas";
        if (lower.contains("/apartments/") || lower.contains("\\apartments\\")) return "apartments";
        if (lower.contains("/airbnbs/") || lower.contains("\\airbnbs\\")) return "airbnbs";

        // Check filename
        if (lower.contains("hotel")) return "hotels";
        if (lower.contains("villa")) return "villas";
        if (lower.contains("apartment")) return "apartments";
        if (lower.contains("airbnb")) return "airbnbs";

        return null;
    }

    /**
     * Recursively search for a file in a directory
     */
    private static Path findFileInDirectory(Path directory, String filename) {
        try {
            return Files.walk(directory, 4) // Search up to 4 levels deep
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.warning("Error searching for file '" + filename + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Diagnostic method - lists all images in uploads directory
     */
    public static void listAllImagesInUploads() {
        LOGGER.info("\n========== Images in Uploads Directory ==========");
        try {
            Path uploadsPath = Paths.get(UPLOADS_DIR);
            if (!Files.exists(uploadsPath)) {
                LOGGER.warning("Uploads directory does not exist!");
                return;
            }

            Files.walk(uploadsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                name.endsWith(".png") || name.endsWith(".gif") ||
                                name.endsWith(".webp");
                    })
                    .forEach(path -> {
                        Path relativePath = Paths.get(PROJECT_ROOT).relativize(path);
                        LOGGER.info("   " + normalizePathForStorage(relativePath.toString()));
                    });
        } catch (Exception e) {
            LOGGER.severe("Error listing images: " + e.getMessage());
        }
        LOGGER.info("==================================================\n");
    }
}